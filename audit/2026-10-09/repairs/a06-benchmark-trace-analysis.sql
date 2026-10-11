-- Identify the app by its exact surface layer: early process metadata can retain a zygote name.
WITH app_processes AS (SELECT DISTINCT upid FROM actual_frame_timeline_slice
  WHERE layer_name LIKE 'TX - com.saaf.milkshop.qa.benchmark/androidx.activity.ComponentActivity%')
SELECT p.name AS captured_process_name, f.jank_type, f.jank_tag, f.present_type, f.on_time_finish,
       COUNT(*) AS frames, ROUND(AVG(f.dur)/1000000.0,3) AS average_ms,
       ROUND(MAX(f.dur)/1000000.0,3) AS max_ms
FROM actual_frame_timeline_slice f JOIN process p USING(upid)
WHERE f.upid IN (SELECT upid FROM app_processes) AND f.dur>=0
GROUP BY p.name,f.jank_type,f.jank_tag,f.present_type,f.on_time_finish
ORDER BY frames DESC;

WITH app_processes AS (SELECT DISTINCT upid FROM actual_frame_timeline_slice
  WHERE layer_name LIKE 'TX - com.saaf.milkshop.qa.benchmark/androidx.activity.ComponentActivity%')
SELECT t.name AS thread, ROUND(SUM(s.dur)/1000000.0,3) AS scheduled_cpu_ms
FROM sched s JOIN thread t USING(utid) JOIN process p USING(upid)
WHERE p.upid IN (SELECT upid FROM app_processes) AND s.dur>=0
GROUP BY t.name ORDER BY scheduled_cpu_ms DESC LIMIT 12;

WITH app_processes AS (SELECT DISTINCT upid FROM actual_frame_timeline_slice
  WHERE layer_name LIKE 'TX - com.saaf.milkshop.qa.benchmark/androidx.activity.ComponentActivity%')
SELECT t.name AS thread,s.name,COUNT(*) AS slices,ROUND(AVG(s.dur)/1000000.0,3) AS average_ms,
       ROUND(MAX(s.dur)/1000000.0,3) AS max_ms
FROM slice s JOIN thread_track tr ON tr.id=s.track_id JOIN thread t ON t.utid=tr.utid JOIN process p USING(upid)
WHERE p.upid IN (SELECT upid FROM app_processes) AND s.dur>=0 AND
      (s.name LIKE '%doFrame%' OR s.name LIKE '%performTraversals%' OR s.name LIKE '%measure%' OR s.name LIKE '%DrawFrame%' OR s.name LIKE '%GC%')
GROUP BY t.name,s.name ORDER BY max_ms DESC LIMIT 20;

SELECT name,severity,value FROM stats WHERE value<>0 AND severity IN ('error','data_loss') ORDER BY name;
