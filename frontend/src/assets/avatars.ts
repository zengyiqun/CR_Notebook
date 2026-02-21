function svg(bg: string, content: string): string {
  return `data:image/svg+xml,${encodeURIComponent(`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect width="100" height="100" rx="50" fill="${bg}"/>${content}</svg>`)}`
}

export const presetAvatars: { id: string; name: string; url: string }[] = [
  {
    id: 'rabbit',
    name: '兔子',
    url: svg('#a78bfa',
      '<ellipse cx="38" cy="28" rx="6" ry="16" fill="white"/><ellipse cx="62" cy="28" rx="6" ry="16" fill="white"/><ellipse cx="38" cy="28" rx="3.5" ry="12" fill="#c4b5fd"/><ellipse cx="62" cy="28" rx="3.5" ry="12" fill="#c4b5fd"/><ellipse cx="50" cy="56" rx="18" ry="15" fill="white"/><circle cx="44" cy="53" r="3" fill="#6d28d9"/><circle cx="56" cy="53" r="3" fill="#6d28d9"/><ellipse cx="50" cy="59" rx="3" ry="2" fill="#f9a8d4"/>')
  },
  {
    id: 'cat',
    name: '猫咪',
    url: svg('#fbbf24',
      '<polygon points="30,30 38,55 22,55" fill="white"/><polygon points="70,30 78,55 62,55" fill="white"/><polygon points="32,32 37,52 25,52" fill="#fde68a"/><polygon points="68,32 75,52 63,52" fill="#fde68a"/><ellipse cx="50" cy="58" rx="20" ry="16" fill="white"/><circle cx="43" cy="55" r="3" fill="#92400e"/><circle cx="57" cy="55" r="3" fill="#92400e"/><ellipse cx="50" cy="61" rx="2.5" ry="1.5" fill="#f9a8d4"/><line x1="25" y1="56" x2="40" y2="58" stroke="#92400e" stroke-width="1"/><line x1="25" y1="62" x2="40" y2="61" stroke="#92400e" stroke-width="1"/><line x1="75" y1="56" x2="60" y2="58" stroke="#92400e" stroke-width="1"/><line x1="75" y1="62" x2="60" y2="61" stroke="#92400e" stroke-width="1"/>')
  },
  {
    id: 'dog',
    name: '狗狗',
    url: svg('#f97316',
      '<ellipse cx="32" cy="42" rx="10" ry="14" fill="#fed7aa"/><ellipse cx="68" cy="42" rx="10" ry="14" fill="#fed7aa"/><ellipse cx="50" cy="58" rx="20" ry="17" fill="white"/><circle cx="43" cy="54" r="3.5" fill="#7c2d12"/><circle cx="57" cy="54" r="3.5" fill="#7c2d12"/><ellipse cx="50" cy="62" rx="4" ry="3" fill="#1c1917"/><path d="M46 66 Q50 70 54 66" stroke="#7c2d12" fill="none" stroke-width="1.5"/>')
  },
  {
    id: 'bear',
    name: '小熊',
    url: svg('#78716c',
      '<circle cx="32" cy="38" r="10" fill="#a8a29e"/><circle cx="68" cy="38" r="10" fill="#a8a29e"/><circle cx="32" cy="38" r="6" fill="#d6d3d1"/><circle cx="68" cy="38" r="6" fill="#d6d3d1"/><ellipse cx="50" cy="58" rx="22" ry="18" fill="#d6d3d1"/><circle cx="43" cy="53" r="3" fill="#292524"/><circle cx="57" cy="53" r="3" fill="#292524"/><ellipse cx="50" cy="60" rx="4" ry="3" fill="#292524"/>')
  },
  {
    id: 'panda',
    name: '熊猫',
    url: svg('#e5e7eb',
      '<circle cx="32" cy="38" r="10" fill="#1f2937"/><circle cx="68" cy="38" r="10" fill="#1f2937"/><ellipse cx="50" cy="58" rx="22" ry="18" fill="white"/><ellipse cx="42" cy="53" rx="7" ry="6" fill="#1f2937"/><ellipse cx="58" cy="53" rx="7" ry="6" fill="#1f2937"/><circle cx="42" cy="53" r="3" fill="white"/><circle cx="58" cy="53" r="3" fill="white"/><ellipse cx="50" cy="63" rx="3" ry="2" fill="#1f2937"/>')
  },
  {
    id: 'fox',
    name: '狐狸',
    url: svg('#ea580c',
      '<polygon points="25,30 40,55 20,55" fill="#fed7aa"/><polygon points="75,30 80,55 60,55" fill="#fed7aa"/><ellipse cx="50" cy="58" rx="20" ry="16" fill="white"/><circle cx="43" cy="54" r="2.5" fill="#7c2d12"/><circle cx="57" cy="54" r="2.5" fill="#7c2d12"/><ellipse cx="50" cy="60" rx="2" ry="1.5" fill="#1c1917"/><path d="M35 64 Q50 72 65 64" stroke="white" fill="white" stroke-width="0"/>')
  },
  {
    id: 'penguin',
    name: '企鹅',
    url: svg('#1e3a5f',
      '<ellipse cx="50" cy="55" rx="24" ry="22" fill="#1e3a5f"/><ellipse cx="50" cy="60" rx="16" ry="16" fill="white"/><circle cx="42" cy="48" r="3" fill="white"/><circle cx="58" cy="48" r="3" fill="white"/><circle cx="42" cy="48" r="1.5" fill="#0f172a"/><circle cx="58" cy="48" r="1.5" fill="#0f172a"/><polygon points="50,54 46,60 54,60" fill="#f59e0b"/>')
  },
  {
    id: 'owl',
    name: '猫头鹰',
    url: svg('#92400e',
      '<polygon points="35,30 42,45 28,45" fill="#78350f"/><polygon points="65,30 72,45 58,45" fill="#78350f"/><ellipse cx="50" cy="58" rx="22" ry="18" fill="#fef3c7"/><circle cx="42" cy="52" r="8" fill="white"/><circle cx="58" cy="52" r="8" fill="white"/><circle cx="42" cy="52" r="4" fill="#78350f"/><circle cx="58" cy="52" r="4" fill="#78350f"/><polygon points="50,58 47,64 53,64" fill="#f59e0b"/>')
  },
  {
    id: 'koala',
    name: '考拉',
    url: svg('#9ca3af',
      '<circle cx="30" cy="45" r="12" fill="#d1d5db"/><circle cx="70" cy="45" r="12" fill="#d1d5db"/><circle cx="30" cy="45" r="7" fill="#f3f4f6"/><circle cx="70" cy="45" r="7" fill="#f3f4f6"/><ellipse cx="50" cy="58" rx="20" ry="17" fill="#e5e7eb"/><circle cx="43" cy="54" r="2.5" fill="#1f2937"/><circle cx="57" cy="54" r="2.5" fill="#1f2937"/><ellipse cx="50" cy="61" rx="5" ry="4" fill="#374151"/>')
  },
  {
    id: 'frog',
    name: '青蛙',
    url: svg('#16a34a',
      '<circle cx="38" cy="38" r="10" fill="#22c55e"/><circle cx="62" cy="38" r="10" fill="#22c55e"/><circle cx="38" cy="36" r="5" fill="white"/><circle cx="62" cy="36" r="5" fill="white"/><circle cx="38" cy="36" r="2.5" fill="#14532d"/><circle cx="62" cy="36" r="2.5" fill="#14532d"/><ellipse cx="50" cy="60" rx="22" ry="14" fill="#4ade80"/><path d="M38 62 Q50 70 62 62" stroke="#14532d" fill="none" stroke-width="2"/>')
  },
  {
    id: 'lion',
    name: '狮子',
    url: svg('#d97706',
      '<circle cx="50" cy="52" r="28" fill="#fbbf24"/><ellipse cx="50" cy="56" rx="18" ry="16" fill="#fef3c7"/><circle cx="43" cy="52" r="3" fill="#78350f"/><circle cx="57" cy="52" r="3" fill="#78350f"/><ellipse cx="50" cy="58" rx="3" ry="2" fill="#78350f"/><path d="M44 64 Q50 68 56 64" stroke="#78350f" fill="none" stroke-width="1.5"/>')
  },
  {
    id: 'duck',
    name: '小鸭',
    url: svg('#facc15',
      '<ellipse cx="50" cy="55" rx="22" ry="18" fill="#fef08a"/><circle cx="42" cy="48" r="3" fill="#1c1917"/><circle cx="58" cy="48" r="3" fill="#1c1917"/><ellipse cx="50" cy="60" rx="6" ry="4" fill="#f97316"/><path d="M30 40 Q25 30 35 35" stroke="#fef08a" fill="#fef08a"/><path d="M70 40 Q75 30 65 35" stroke="#fef08a" fill="#fef08a"/>')
  },
]
