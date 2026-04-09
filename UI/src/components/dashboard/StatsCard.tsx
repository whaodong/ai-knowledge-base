// 统计卡片组件
interface StatsCardProps {
  title: string
  value: string | number
  subtitle?: string
  icon: React.ReactNode
  trend?: {
    value: number
    direction: 'up' | 'down'
  }
  color?: 'blue' | 'green' | 'yellow' | 'red' | 'purple'
}

const colorMap = {
  blue: {
    bg: 'bg-blue-100 dark:bg-blue-900/30',
    text: 'text-blue-500',
    trendUp: 'text-green-500',
    trendDown: 'text-red-500'
  },
  green: {
    bg: 'bg-green-100 dark:bg-green-900/30',
    text: 'text-green-500',
    trendUp: 'text-green-500',
    trendDown: 'text-red-500'
  },
  yellow: {
    bg: 'bg-yellow-100 dark:bg-yellow-900/30',
    text: 'text-yellow-500',
    trendUp: 'text-green-500',
    trendDown: 'text-red-500'
  },
  red: {
    bg: 'bg-red-100 dark:bg-red-900/30',
    text: 'text-red-500',
    trendUp: 'text-green-500',
    trendDown: 'text-red-500'
  },
  purple: {
    bg: 'bg-purple-100 dark:bg-purple-900/30',
    text: 'text-purple-500',
    trendUp: 'text-green-500',
    trendDown: 'text-red-500'
  }
}

const StatsCard = ({ 
  title, 
  value, 
  subtitle, 
  icon, 
  trend,
  color = 'blue' 
}: StatsCardProps) => {
  const colors = colorMap[color]

  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-5 border border-gray-200 dark:border-gray-700">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm font-medium text-gray-500 dark:text-gray-400">{title}</p>
          <div className="mt-2 flex items-baseline gap-2">
            <p className="text-2xl font-bold text-gray-900 dark:text-white">{value}</p>
            {trend && (
              <span className={`text-sm font-medium ${trend.direction === 'up' ? colors.trendUp : colors.trendDown}`}>
                {trend.direction === 'up' ? '↑' : '↓'} {Math.abs(trend.value)}%
              </span>
            )}
          </div>
          {subtitle && (
            <p className="mt-1 text-xs text-gray-400 dark:text-gray-500">{subtitle}</p>
          )}
        </div>
        <div className={`p-3 rounded-lg ${colors.bg}`}>
          <div className={colors.text}>
            {icon}
          </div>
        </div>
      </div>
    </div>
  )
}

export default StatsCard
