package com.saaf.milkshop.statements

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.saaf.milkshop.R
import com.saaf.milkshop.application.owner.CustomerStatement
import com.saaf.milkshop.application.owner.statementMoney
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal enum class StatementTextRole(val size: Float) { Brand(17f), Heading(13f), Body(11f), Support(10f), Metric(12f), Product(10f) }
internal data class StatementText(val text: String,val x: Float,val y: Float,val width: Int,val height: Int,val role: StatementTextRole)
internal data class StatementDocumentPage(val texts: List<StatementText>,val rules: List<Float>)
internal data class StatementDocument(val statement: CustomerStatement,val pages: List<StatementDocumentPage>)

/** One measured page plan and Canvas renderer for the preview, PDF and numbered PNGs. */
internal class StatementDocumentEngine(context: Context) {
    private val regular=requireNotNull(ResourcesCompat.getFont(context,R.font.manrope_regular))
    private val semibold=requireNotNull(ResourcesCompat.getFont(context,R.font.manrope_semibold))
    private val bold=requireNotNull(ResourcesCompat.getFont(context,R.font.manrope_bold))
    private fun paint(role: StatementTextRole)=TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize=role.size
        typeface=when(role) {StatementTextRole.Brand,StatementTextRole.Metric->bold;StatementTextRole.Heading->semibold;else->regular}
        color=when(role) {StatementTextRole.Brand->Color.rgb(23,63,50);StatementTextRole.Metric->Color.rgb(98,73,14)
            StatementTextRole.Support,StatementTextRole.Product->Color.rgb(81,95,85);else->Color.rgb(23,32,27)}
    }
    private fun layout(text: String,role: StatementTextRole,width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text,0,text.length,paint(role),width).setIncludePad(false)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(2f,1f).build()

    fun plan(statement: CustomerStatement,cancel: ()->Unit = {}): StatementDocument {
        require(statement.currency=="PKR") {"This statement currency is not supported."}
        var balance=statement.openingPaisa
        statement.lines.forEach {row->
            balance=Math.addExact(balance,row.movementPaisa)
            require(balance==row.balancePaisa) {"Statement rows do not reconcile. Refresh the statement."}
        }
        require(balance==statement.closingPaisa) {"Statement total does not reconcile. Refresh the statement."}
        val pages=mutableListOf<StatementDocumentPage>()
        var texts=mutableListOf<StatementText>();var rules=mutableListOf<Float>();var y=MARGIN
        var carried=statement.openingPaisa
        fun addFixed(text: String,role: StatementTextRole) {
            require(text.length<=8000) {"Shop or customer details are too long. Shorten them before exporting."}
            val l=layout(text,role,CONTENT_WIDTH)
            texts.add(StatementText(text,MARGIN,y,CONTENT_WIDTH,l.height,role));y+=l.height+6
        }
        fun header(continued: String?=null) {
            y=MARGIN
            addFixed(statement.shopName,StatementTextRole.Brand)
            statement.shopPhone?.takeIf {it.isNotBlank()}?.let {addFixed(it,StatementTextRole.Support)}
            statement.shopAddress?.takeIf {it.isNotBlank()}?.let {addFixed(it,StatementTextRole.Support)}
            addFixed("Customer statement",StatementTextRole.Heading)
            addFixed(statement.customerName,StatementTextRole.Body)
            addFixed(statement.period.label.replace('–','-'),StatementTextRole.Support)
            addFixed("${if(pages.isEmpty()) "Opening balance" else "Brought forward"}: ${statementMoney(carried)}",StatementTextRole.Metric)
            require(y<400) {"Shop or customer details are too long to fit safely. Shorten them before exporting."}
            rules.add(y);y+=12
            continued?.let { addFixed("Continued entry $it",StatementTextRole.Support) }
        }
        fun next(continued: String?) {
            pages.add(StatementDocumentPage(texts.toList(),rules.toList()))
            require(pages.size<MAX_PAGES) {"Statement exceeds $MAX_PAGES pages. Choose a smaller period."}
            texts=mutableListOf();rules=mutableListOf();header(continued)
        }
        fun add(text: String,role: StatementTextRole,reference: String?=null) {
            require(text.length<=50000) {"One entry is too long. Review its details before exporting."}
            var remaining=text
            do {
                cancel()
                val x=if(role==StatementTextRole.Product) MARGIN+12 else MARGIN
                val width=(WIDTH-MARGIN-x).toInt()
                val measured=layout(remaining,role,width)
                val fit=(0 until measured.lineCount).takeWhile {measured.getLineBottom(it)<=BODY_BOTTOM-y}.size
                if(fit==0) {next(reference);continue}
                val end=measured.getLineEnd(fit-1)
                val part=remaining.substring(0,end).trimEnd('\n')
                val drawn=layout(part,role,width)
                texts.add(StatementText(part,x,y,width,drawn.height,role));y+=drawn.height+6
                remaining=remaining.substring(end)
                if(remaining.isNotEmpty()) next(reference)
            } while(remaining.isNotEmpty())
        }
        header()
        if(statement.lines.isEmpty()) add("No activity in this period.",StatementTextRole.Body)
        statement.lines.forEach { row ->
            cancel()
            // Keep a routine entry together when it fits; unusually long entries continue explicitly.
            val parts=listOfNotNull("${row.date} - ${row.description}" to StatementTextRole.Heading,
                row.productText?.let {it to StatementTextRole.Product},
                "${if(row.movementPaisa>0) "+" else ""}${statementMoney(row.movementPaisa)}" to StatementTextRole.Metric,
                "Balance ${statementMoney(row.balancePaisa)} | Ref ${row.reference}" to StatementTextRole.Support)
            val height=parts.sumOf { (text,role)->layout(text,role,if(role==StatementTextRole.Product) CONTENT_WIDTH-12 else CONTENT_WIDTH).height+6 }+10
            if(y+height>BODY_BOTTOM && height<300) next(null)
            parts.forEach {(text,role)->add(text,role,row.reference)}
            carried=row.balancePaisa
            if(y+8<=BODY_BOTTOM) {rules.add(y);y+=10}
        }
        add("${statement.closingLabel}: ${statementMoney(statement.closingPaisa)}",StatementTextRole.Metric)
        if(statement.lines.any {it.roundingAdjusted}) add("Movements reflect changes in the rounded running balance so displayed amounts reconcile.",StatementTextRole.Support)
        pages.add(StatementDocumentPage(texts.toList(),rules.toList()))
        require(pages.all {p->p.texts.all {it.y+it.height<=BODY_BOTTOM}}) {"Statement content could not fit safely. Choose a smaller period."}
        val generated=Instant.parse(statement.generatedAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM uuuu, HH:mm"))
        val complete=pages.mapIndexed {index,p->
            val footer=listOf("Prepared $generated | Local records", "Product names are current catalog labels.","Page ${index+1} of ${pages.size}")
            p.copy(texts=p.texts+footer.mapIndexed {n,t->val l=layout(t,StatementTextRole.Support,CONTENT_WIDTH)
                StatementText(t,MARGIN,780f+n*16,CONTENT_WIDTH,l.height,StatementTextRole.Support)})
        }
        require(complete.all {p->p.texts.all {it.y+it.height<=HEIGHT-12 && it.x+it.width<=WIDTH-MARGIN}}) {"Statement could not fit safely. Choose a smaller period."}
        return StatementDocument(statement,complete)
    }

    fun draw(document: StatementDocument,page: Int,canvas: Canvas,cancel: ()->Unit = {}) {
        canvas.drawColor(PAPER_COLOR)
        val p=document.pages[page]
        val line=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=Color.rgb(201,195,179);strokeWidth=0.6f}
        p.rules.forEach {canvas.drawLine(MARGIN,it,WIDTH-MARGIN,it,line)}
        p.texts.forEach {block->
            cancel()
            if(block.role==StatementTextRole.Product) {
                val marker=Paint(Paint.ANTI_ALIAS_FLAG).apply {color=Color.rgb(23,63,50);style=Paint.Style.STROKE;strokeWidth=1f}
                canvas.drawRoundRect(MARGIN,block.y+3,MARGIN+6,block.y+9,1f,1f,marker)
            }
            canvas.save();canvas.translate(block.x,block.y);layout(block.text,block.role,block.width).draw(canvas);canvas.restore()
        }
    }
    fun bitmap(document: StatementDocument,page: Int,cancel: ()->Unit = {}): Bitmap {
        val bitmap=try { Bitmap.createBitmap(WIDTH*2,HEIGHT*2,Bitmap.Config.ARGB_8888) }
            catch(failure: OutOfMemoryError) {throw IllegalStateException("Not enough memory to render a page. Close another app and try again.",failure)}
        try {val canvas=Canvas(bitmap);canvas.scale(2f,2f);draw(document,page,canvas,cancel);return bitmap}
        catch(failure: Throwable) {bitmap.recycle();throw failure}
    }
    fun pdf(document: StatementDocument,output: OutputStream,cancel: ()->Unit = {}) {
        val pdf=PdfDocument()
        try {
            document.pages.indices.forEach {index->
                cancel();val page=pdf.startPage(PdfDocument.PageInfo.Builder(WIDTH,HEIGHT,index+1).create())
                try {draw(document,index,page.canvas,cancel)} finally {pdf.finishPage(page)}
            }
            cancel();pdf.writeTo(output)
        } finally {pdf.close()}
    }
    companion object {
        const val WIDTH=595
        const val HEIGHT=842
        const val MAX_PAGES=40
        const val PAPER_COLOR=0xFFFFFDF7.toInt()
        private const val MARGIN=36f
        private const val CONTENT_WIDTH=523
        private const val BODY_BOTTOM=756f
    }
}
