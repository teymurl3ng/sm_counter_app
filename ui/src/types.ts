export interface TaskItem {
  id: string;
  title: string;
  category: 'core' | 'validation' | 'deployment' | 'optimization';
  status: 'completed' | 'in_progress' | 'pending';
  priority: 'high' | 'medium' | 'low';
  updatedAt: string;
}

export interface MetricStat {
  id: string;
  label: string;
  value: string;
  change: string;
  trend: 'up' | 'down' | 'neutral';
}
