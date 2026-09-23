package com.saaf.milkshop.nativeui.owner

import com.saaf.milkshop.application.owner.NativeOwnerProductsFacade
import com.saaf.milkshop.domain.ProductCatalogInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns the Products destination, its transient UI state, and product requests. */
internal class OwnerProductsController(
    private val facade: NativeOwnerProductsFacade,
    private val runtime: OwnerMoreRuntime,
) {
    private var readJob: Job? = null

    fun open() {
        val token = runtime.captureSession() ?: return
        runtime.updateReady { current ->
            current.copy(
                destination = NativeOwnerMoreDestination.Products,
                productsLoading = NativeOwnerMoreDestination.Products !in current.loadedDestinations,
                productsShowArchived = false,
                productDetail = null,
                productCreateOpen = false,
                productCreating = false,
                productEditOpen = false,
                productUpdating = false,
                productArchiving = false,
                productUnarchiving = false,
                productsError = null,
                customersLoading = false,
                customerDetail = null,
                customerCreateOpen = false,
                customerCreating = false,
                customerEditOpen = false,
                customerUpdating = false,
                customersError = null,
                suppliersLoading = false,
                supplierDetail = null,
                supplierCreateOpen = false,
                supplierCreating = false,
                supplierEditOpen = false,
                supplierUpdating = false,
                suppliersError = null,
                employeesLoading = false,
                employeeDetail = null,
                employeeCreateOpen = false,
                employeeCreating = false,
                employeeEditOpen = false,
                employeeUpdating = false,
                employeesError = null,
                confirmRemoveUnlock = false,
                error = null,
                saving = false,
            )
        }
        loadProducts(token, includeArchived = false)
    }

    fun refresh() {
        val token = runtime.captureSession() ?: return
        val current = runtime.ready() ?: return
        if (current.destination != NativeOwnerMoreDestination.Products) return
        val showArchived = current.productsShowArchived
        runtime.updateReady {
            it.copy(
                productsLoading = true,
                productsError = null,
                productDetail = null,
                productCreateOpen = false,
                productCreating = false,
                productEditOpen = false,
                productUpdating = false,
                productArchiving = false,
                productUnarchiving = false,
            )
        }
        loadProducts(token, includeArchived = showArchived)
    }

    fun setShowArchived(showArchived: Boolean) {
        val token = runtime.captureSession() ?: return
        val current = runtime.ready() ?: return
        if (current.destination != NativeOwnerMoreDestination.Products) return
        if (current.productsShowArchived == showArchived &&
            !current.productsLoading &&
            current.productDetail == null
        ) {
            return
        }
        runtime.updateReady {
            it.copy(
                productsShowArchived = showArchived,
                productsLoading = true,
                productsError = null,
                productDetail = null,
                productCreateOpen = false,
                productCreating = false,
                productEditOpen = false,
                productUpdating = false,
                productArchiving = false,
                productUnarchiving = false,
            )
        }
        loadProducts(token, includeArchived = showArchived)
    }

    fun openDetail(productId: Long) {
        val token = runtime.captureSession() ?: return
        val current = runtime.ready() ?: return
        if (current.destination != NativeOwnerMoreDestination.Products) return
        runtime.updateReady {
            it.copy(
                productsLoading = true,
                productsError = null,
                productCreateOpen = false,
                productCreating = false,
                productEditOpen = false,
                productUpdating = false,
                productArchiving = false,
                productUnarchiving = false,
            )
        }
        readJob?.cancel()
        readJob = runtime.scope.launch {
            try {
                val detail = withContext(Dispatchers.IO) {
                    facade.loadDetail(token.handle, productId)
                }
                runtime.updateCurrent(token, NativeOwnerMoreDestination.Products) {
                    it.copy(
                        productDetail = detail,
                        productsLoading = false,
                        productsError = null,
                    )
                }
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun clearDetail() {
        readJob?.cancel()
        runtime.updateReady { current ->
            current.copy(
                productDetail = null,
                productEditOpen = false,
                productUpdating = false,
                productArchiving = false,
                productUnarchiving = false,
                productsError = null,
                productsLoading = false,
            )
        }
    }

    fun openCreate() {
        val current = runtime.ready() ?: return
        if (current.destination != NativeOwnerMoreDestination.Products) return
        if (current.productsShowArchived) return
        runtime.updateReady {
            it.copy(
                productCreateOpen = true,
                productCreating = false,
                productEditOpen = false,
                productUpdating = false,
                productArchiving = false,
                productUnarchiving = false,
                productDetail = null,
                productsError = null,
                productsLoading = false,
            )
        }
    }

    fun clearCreate() {
        runtime.updateReady {
            it.copy(
                productCreateOpen = false,
                productCreating = false,
                productEditOpen = false,
                productUpdating = false,
                productArchiving = false,
                productUnarchiving = false,
                productsError = null,
            )
        }
    }

    fun create(input: ProductCatalogInput) {
        val token = runtime.captureSession() ?: return
        val current = runtime.ready() ?: return
        if (current.destination != NativeOwnerMoreDestination.Products || !current.productCreateOpen) return
        if (current.productCreating) return
        runtime.updateReady { it.copy(productCreating = true, productsError = null) }
        readJob?.cancel()
        runtime.scope.launch {
            try {
                withContext(Dispatchers.IO) { facade.createProduct(token.handle, input) }
                val snapshot = withContext(Dispatchers.IO) {
                    facade.loadProducts(token.handle, includeArchived = false)
                }
                runtime.updateCurrent(token, NativeOwnerMoreDestination.Products) {
                    it.copy(
                        products = snapshot.products,
                        productsShowArchived = false,
                        productCreateOpen = false,
                        productCreating = false,
                        productEditOpen = false,
                        productUpdating = false,
                        productArchiving = false,
                        productUnarchiving = false,
                        productsError = null,
                        productDetail = null,
                        productsLoading = false,
                    )
                }
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun openEdit() {
        val current = runtime.ready() ?: return
        if (current.destination != NativeOwnerMoreDestination.Products) return
        val detail = current.productDetail ?: return
        if (detail.row.archived) return
        runtime.updateReady {
            it.copy(
                productEditOpen = true,
                productUpdating = false,
                productCreateOpen = false,
                productCreating = false,
                productArchiving = false,
                productUnarchiving = false,
                productsError = null,
                productsLoading = false,
            )
        }
    }

    fun clearEdit() {
        runtime.updateReady {
            it.copy(
                productEditOpen = false,
                productUpdating = false,
                productsError = null,
            )
        }
    }

    fun update(input: ProductCatalogInput) {
        val token = runtime.captureSession() ?: return
        val current = runtime.ready() ?: return
        val productId = current.productDetail?.row?.productId ?: return
        if (current.destination != NativeOwnerMoreDestination.Products || !current.productEditOpen) return
        if (current.productUpdating) return
        val showArchived = current.productsShowArchived
        runtime.updateReady { it.copy(productUpdating = true, productsError = null) }
        readJob?.cancel()
        runtime.scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    facade.updateProduct(token.handle, productId, input)
                }
                val detail = withContext(Dispatchers.IO) {
                    facade.loadDetail(token.handle, productId)
                }
                val snapshot = withContext(Dispatchers.IO) {
                    facade.loadProducts(token.handle, includeArchived = showArchived)
                }
                runtime.updateCurrent(token, NativeOwnerMoreDestination.Products) {
                    it.copy(
                        products = snapshot.products,
                        productDetail = detail,
                        productEditOpen = false,
                        productUpdating = false,
                        productsError = null,
                        productsLoading = false,
                    )
                }
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    fun archive() = setArchived(archived = true)

    fun unarchive() = setArchived(archived = false)

    fun resetTransient() {
        readJob?.cancel()
        runtime.updateReady {
            it.copy(
                productsLoading = false,
                productsShowArchived = false,
                productDetail = null,
                productCreateOpen = false,
                productCreating = false,
                productEditOpen = false,
                productUpdating = false,
                productArchiving = false,
                productUnarchiving = false,
                productsError = null,
            )
        }
    }

    private fun setArchived(archived: Boolean) {
        val token = runtime.captureSession() ?: return
        val current = runtime.ready() ?: return
        val productId = current.productDetail?.row?.productId ?: return
        if (current.destination != NativeOwnerMoreDestination.Products) return
        if (current.productDetail?.row?.archived == archived) return
        if (current.productArchiving || current.productUnarchiving ||
            current.productEditOpen || current.productCreateOpen
        ) {
            return
        }
        val showArchived = current.productsShowArchived
        runtime.updateReady {
            if (archived) {
                it.copy(productArchiving = true, productsError = null)
            } else {
                it.copy(productUnarchiving = true, productsError = null)
            }
        }
        readJob?.cancel()
        runtime.scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (archived) {
                        facade.archiveProduct(token.handle, productId)
                    } else {
                        facade.unarchiveProduct(token.handle, productId)
                    }
                }
                val snapshot = withContext(Dispatchers.IO) {
                    facade.loadProducts(token.handle, includeArchived = showArchived)
                }
                runtime.updateCurrent(token, NativeOwnerMoreDestination.Products) {
                    it.copy(
                        products = snapshot.products,
                        productDetail = null,
                        productEditOpen = false,
                        productUpdating = false,
                        productArchiving = false,
                        productUnarchiving = false,
                        productsError = null,
                        productsLoading = false,
                    )
                }
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    private fun loadProducts(token: OwnerSessionToken, includeArchived: Boolean) {
        readJob?.cancel()
        readJob = runtime.scope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) {
                    facade.loadProducts(token.handle, includeArchived)
                }
                runtime.updateCurrent(token, NativeOwnerMoreDestination.Products) {
                    it.copy(
                        products = snapshot.products,
                        productsLoading = false,
                        productsError = null,
                        loadedDestinations = it.loadedDestinations + NativeOwnerMoreDestination.Products,
                    )
                }
            } catch (error: Throwable) {
                handleFailure(token, error)
            }
        }
    }

    private fun handleFailure(token: OwnerSessionToken, error: Throwable) {
        runtime.handleFailure(token, NativeOwnerMoreDestination.Products, error) { current, message ->
            current.copy(
                productsLoading = false,
                productCreating = false,
                productUpdating = false,
                productArchiving = false,
                productUnarchiving = false,
                productsError = message,
            )
        }
    }
}
