import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import type { ThemeMode } from '@/types'

export type AccentColor = 'indigo' | 'violet' | 'rose' | 'emerald' | 'amber' | 'sky'

const ACCENT_PRESETS: Record<AccentColor, { accent: string; accentLight: string; accentLightDark: string }> = {
  indigo:  { accent: '#6366f1', accentLight: '#e0e7ff', accentLightDark: '#312e81' },
  violet:  { accent: '#8b5cf6', accentLight: '#ede9fe', accentLightDark: '#3b0764' },
  rose:    { accent: '#f43f5e', accentLight: '#ffe4e6', accentLightDark: '#4c0519' },
  emerald: { accent: '#10b981', accentLight: '#d1fae5', accentLightDark: '#064e3b' },
  amber:   { accent: '#f59e0b', accentLight: '#fef3c7', accentLightDark: '#451a03' },
  sky:     { accent: '#0ea5e9', accentLight: '#e0f2fe', accentLightDark: '#0c4a6e' },
}

export const useThemeStore = defineStore('theme', () => {
  const mode = ref<ThemeMode>(
    (localStorage.getItem('theme') as ThemeMode) || 'light'
  )
  const accentColor = ref<AccentColor>(
    (localStorage.getItem('accent-color') as AccentColor) || 'indigo'
  )

  function toggle() {
    mode.value = mode.value === 'light' ? 'dark' : 'light'
  }

  function setAccent(color: AccentColor) {
    accentColor.value = color
  }

  function applyAccent(color: AccentColor) {
    const preset = ACCENT_PRESETS[color]
    document.documentElement.style.setProperty('--color-craft-accent', preset.accent)
    document.documentElement.style.setProperty('--color-craft-accent-light', preset.accentLight)
    if (mode.value === 'dark') {
      document.documentElement.style.setProperty('--color-craft-accent-light', preset.accentLightDark)
    }
    localStorage.setItem('accent-color', color)
    updateFavicon(preset.accent, mode.value === 'dark' ? preset.accentLightDark : preset.accentLight)
  }

  function updateFavicon(accent: string, accentLight: string) {
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64"><rect width="64" height="64" rx="14" fill="${accent}"/><ellipse cx="24" cy="16" rx="5" ry="12" fill="white"/><ellipse cx="40" cy="16" rx="5" ry="12" fill="white"/><ellipse cx="24" cy="16" rx="3" ry="9" fill="${accentLight}" opacity="0.6"/><ellipse cx="40" cy="16" rx="3" ry="9" fill="${accentLight}" opacity="0.6"/><ellipse cx="32" cy="32" rx="14" ry="12" fill="white"/><circle cx="27" cy="30" r="2.5" fill="${accent}"/><circle cx="37" cy="30" r="2.5" fill="${accent}"/><ellipse cx="32" cy="35" rx="2" ry="1.5" fill="${accentLight}"/><rect x="22" y="42" rx="2" width="20" height="14" fill="white"/><line x1="26" y1="47" x2="38" y2="47" stroke="${accentLight}" stroke-width="1.5" stroke-linecap="round"/><line x1="26" y1="51" x2="34" y2="51" stroke="${accentLight}" stroke-width="1.5" stroke-linecap="round"/></svg>`
    const blob = new Blob([svg], { type: 'image/svg+xml' })
    const url = URL.createObjectURL(blob)
    const link = document.querySelector<HTMLLinkElement>('link[rel="icon"]')
    if (link) {
      if (link.href.startsWith('blob:')) URL.revokeObjectURL(link.href)
      link.href = url
    }
  }

  watch(mode, (val) => {
    localStorage.setItem('theme', val)
    document.documentElement.classList.toggle('dark', val === 'dark')
    applyAccent(accentColor.value)
  }, { immediate: true })

  watch(accentColor, (val) => {
    applyAccent(val)
  }, { immediate: true })

  return { mode, accentColor, toggle, setAccent, accentColors: Object.keys(ACCENT_PRESETS) as AccentColor[] }
})
