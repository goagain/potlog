interface ChipLoaderProps {
  size?: 'sm' | 'md' | 'lg'
}

export default function ChipLoader({ size = 'md' }: ChipLoaderProps) {
  const sizeClasses = {
    sm: 'w-5 h-5',
    md: 'w-10 h-10',
    lg: 'w-16 h-16',
  }

  return (
    <div className={`${sizeClasses[size]} loading-chip`}>
      <svg viewBox="0 0 100 100" className="w-full h-full">
        <circle cx="50" cy="50" r="45" fill="#c41e3a" stroke="#d4af37" strokeWidth="3"/>
        <circle cx="50" cy="50" r="35" fill="none" stroke="#fff" strokeWidth="2" strokeDasharray="8 4"/>
        <circle cx="50" cy="50" r="20" fill="#1a1a1a"/>
      </svg>
    </div>
  )
}
