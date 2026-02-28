export function formatCents(cents: number): string {
  const dollars = cents / 100
  return `$${dollars.toFixed(2)}`
}

export function formatCentsShort(cents: number): string {
  const dollars = cents / 100
  if (dollars >= 1000) {
    return `$${(dollars / 1000).toFixed(1)}K`
  }
  return `$${dollars.toFixed(0)}`
}

export function parseDollarsToCents(dollars: string): number {
  const parsed = parseFloat(dollars)
  if (isNaN(parsed)) return 0
  return Math.round(parsed * 100)
}

export function formatNet(cents: number): string {
  const prefix = cents >= 0 ? '+' : ''
  return `${prefix}${formatCents(cents)}`
}

export function formatNetClass(cents: number): string {
  if (cents > 0) return 'text-green-400'
  if (cents < 0) return 'text-red-400'
  return 'text-gray-400'
}
