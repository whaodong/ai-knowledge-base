// 简单图表组件 - 使用 div/canvas 实现，不引入 echarts

import { useEffect, useRef } from 'react'

interface BarChartProps {
  data: { label: string; value: number }[]
  height?: number
  showValues?: boolean
}

export const BarChart = ({ data, height = 200, showValues = true }: BarChartProps) => {
  const maxValue = Math.max(...data.map(d => d.value), 1)
  
  return (
    <div className="w-full" style={{ height }}>
      <div className="flex items-end justify-around h-full gap-2">
        {data.map((item, idx) => (
          <div key={idx} className="flex flex-col items-center flex-1">
            <div className="w-full flex flex-col items-center">
              {showValues && (
                <span className="text-xs text-gray-500 dark:text-gray-400 mb-1">
                  {item.value}
                </span>
              )}
              <div 
                className="w-full max-w-8 bg-blue-500 rounded-t transition-all duration-300"
                style={{ height: `${(item.value / maxValue) * (height - 40)}px` }}
              />
            </div>
            <span className="mt-2 text-xs text-gray-500 dark:text-gray-400 truncate max-w-full">
              {item.label}
            </span>
          </div>
        ))}
      </div>
    </div>
  )
}

interface LineChartProps {
  data: number[]
  labels?: string[]
  height?: number
  color?: string
}

export const LineChart = ({ data, labels, height = 150, color = '#3B82F6' }: LineChartProps) => {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  
  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas || data.length === 0) return
    
    const ctx = canvas.getContext('2d')
    if (!ctx) return
    
    // 设置 canvas 尺寸
    const dpr = window.devicePixelRatio || 1
    const rect = canvas.getBoundingClientRect()
    canvas.width = rect.width * dpr
    canvas.height = rect.height * dpr
    ctx.scale(dpr, dpr)
    
    const width = rect.width
    const chartHeight = rect.height - 20
    
    // 清除画布
    ctx.clearRect(0, 0, width, rect.height)
    
    const maxValue = Math.max(...data, 1)
    const minValue = Math.min(...data, 0)
    const range = maxValue - minValue || 1
    
    const stepX = width / (data.length - 1 || 1)
    
    // 绘制网格线
    ctx.strokeStyle = '#E5E7EB'
    ctx.lineWidth = 1
    ctx.setLineDash([4, 4])
    for (let i = 0; i <= 4; i++) {
      const y = (chartHeight / 4) * i
      ctx.beginPath()
      ctx.moveTo(0, y)
      ctx.lineTo(width, y)
      ctx.stroke()
    }
    ctx.setLineDash([])
    
    // 绘制线条
    ctx.strokeStyle = color
    ctx.lineWidth = 2
    ctx.beginPath()
    
    data.forEach((value, idx) => {
      const x = idx * stepX
      const y = chartHeight - ((value - minValue) / range) * chartHeight
      
      if (idx === 0) {
        ctx.moveTo(x, y)
      } else {
        ctx.lineTo(x, y)
      }
    })
    ctx.stroke()
    
    // 绘制数据点
    ctx.fillStyle = color
    data.forEach((value, idx) => {
      const x = idx * stepX
      const y = chartHeight - ((value - minValue) / range) * chartHeight
      ctx.beginPath()
      ctx.arc(x, y, 3, 0, Math.PI * 2)
      ctx.fill()
    })
    
  }, [data, color])
  
  return (
    <div className="w-full" style={{ height }}>
      <canvas 
        ref={canvasRef} 
        className="w-full h-full"
        style={{ height: height - 20 }}
      />
      {labels && (
        <div className="flex justify-between mt-1">
          {labels.map((label, idx) => (
            <span key={idx} className="text-xs text-gray-400 dark:text-gray-500">
              {label}
            </span>
          ))}
        </div>
      )}
    </div>
  )
}

interface ProgressRingProps {
  value: number
  max?: number
  size?: number
  strokeWidth?: number
  color?: string
  label?: string
}

export const ProgressRing = ({ 
  value, 
  max = 100, 
  size = 80, 
  strokeWidth = 8,
  color = '#3B82F6',
  label 
}: ProgressRingProps) => {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const percentage = Math.min((value / max) * 100, 100)
  
  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    
    const ctx = canvas.getContext('2d')
    if (!ctx) return
    
    const dpr = window.devicePixelRatio || 1
    canvas.width = size * dpr
    canvas.height = size * dpr
    ctx.scale(dpr, dpr)
    
    const radius = (size - strokeWidth) / 2
    const centerX = size / 2
    const centerY = size / 2
    
    // 背景圆
    ctx.strokeStyle = '#E5E7EB'
    ctx.lineWidth = strokeWidth
    ctx.beginPath()
    ctx.arc(centerX, centerY, radius, 0, Math.PI * 2)
    ctx.stroke()
    
    // 进度圆
    ctx.strokeStyle = color
    ctx.lineWidth = strokeWidth
    ctx.lineCap = 'round'
    ctx.beginPath()
    ctx.arc(
      centerX, 
      centerY, 
      radius, 
      -Math.PI / 2, 
      -Math.PI / 2 + (percentage / 100) * Math.PI * 2
    )
    ctx.stroke()
    
    // 中心文字
    ctx.fillStyle = '#374151'
    ctx.font = `bold ${size / 4}px system-ui`
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(`${Math.round(percentage)}%`, centerX, centerY)
    
  }, [value, max, size, strokeWidth, color])
  
  return (
    <div className="flex flex-col items-center">
      <canvas 
        ref={canvasRef} 
        style={{ width: size, height: size }}
      />
      {label && (
        <span className="mt-1 text-xs text-gray-500 dark:text-gray-400">{label}</span>
      )}
    </div>
  )
}

interface DonutChartProps {
  data: { label: string; value: number; color: string }[]
  size?: number
}

export const DonutChart = ({ data, size = 120 }: DonutChartProps) => {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  
  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas || data.length === 0) return
    
    const ctx = canvas.getContext('2d')
    if (!ctx) return
    
    const dpr = window.devicePixelRatio || 1
    canvas.width = size * dpr
    canvas.height = size * dpr
    ctx.scale(dpr, dpr)
    
    const total = data.reduce((sum, d) => sum + d.value, 0)
    const centerX = size / 2
    const centerY = size / 2
    const outerRadius = size / 2 - 5
    const innerRadius = outerRadius * 0.6
    
    let startAngle = -Math.PI / 2
    
    data.forEach(item => {
      const sliceAngle = (item.value / total) * Math.PI * 2
      
      ctx.fillStyle = item.color
      ctx.beginPath()
      ctx.moveTo(
        centerX + innerRadius * Math.cos(startAngle),
        centerY + innerRadius * Math.sin(startAngle)
      )
      ctx.arc(centerX, centerY, outerRadius, startAngle, startAngle + sliceAngle)
      ctx.arc(centerX, centerY, innerRadius, startAngle + sliceAngle, startAngle, true)
      ctx.closePath()
      ctx.fill()
      
      startAngle += sliceAngle
    })
    
  }, [data, size])
  
  return (
    <div className="flex items-center gap-4">
      <canvas 
        ref={canvasRef} 
        style={{ width: size, height: size }}
      />
      <div className="space-y-1">
        {data.map((item, idx) => (
          <div key={idx} className="flex items-center gap-2 text-sm">
            <span 
              className="w-3 h-3 rounded"
              style={{ backgroundColor: item.color }}
            />
            <span className="text-gray-600 dark:text-gray-400">{item.label}</span>
            <span className="text-gray-500 dark:text-gray-500">{item.value}</span>
          </div>
        ))}
      </div>
    </div>
  )
}
