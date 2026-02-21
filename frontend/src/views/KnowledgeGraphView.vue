/**
 * KnowledgeGraphView — 知识图谱可视化页面。
 *
 * 基于 D3.js 力导向图（force-directed graph）渲染笔记之间的双链关系：
 * - 节点 = 笔记，大小按链接数缩放，颜色按所属文件夹区分
 * - 边 = 双链引用关系，带箭头表示方向
 *
 * 交互功能：拖拽节点、缩放平移、悬停高亮关联、搜索定位、点击查看详情面板
 */
<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { notesApi, type GraphNode, type GraphEdge } from '@/api/notes'
import { useNoteStore } from '@/stores/noteStore'
import * as d3 from 'd3'

const router = useRouter()
const noteStore = useNoteStore()
const svgRef = ref<SVGSVGElement | null>(null)
const containerRef = ref<HTMLDivElement | null>(null)
const loading = ref(true)
const searchQuery = ref('')
const hoveredNode = ref<GraphNode | null>(null)
const selectedNode = ref<GraphNode | null>(null)
const showPanel = ref(false)

interface SimNode extends GraphNode, d3.SimulationNodeDatum {
  linkCount: number
}
interface SimEdge extends d3.SimulationLinkDatum<SimNode> {
  source: SimNode | number
  target: SimNode | number
}

const rawNodes = ref<GraphNode[]>([])
const rawEdges = ref<GraphEdge[]>([])

const stats = computed(() => {
  const isolatedCount = rawNodes.value.filter(n => {
    return !rawEdges.value.some(e => e.source === n.id || e.target === n.id)
  }).length
  return {
    totalNodes: rawNodes.value.length,
    totalEdges: rawEdges.value.length,
    connectedNodes: rawNodes.value.length - isolatedCount,
    isolatedNodes: isolatedCount,
  }
})

const COLORS = [
  '#6366f1', '#8b5cf6', '#ec4899', '#f43f5e',
  '#10b981', '#14b8a6', '#f59e0b', '#0ea5e9',
  '#a855f7', '#ef4444', '#22c55e', '#3b82f6',
]

function getNodeColor(node: GraphNode): string {
  if (node.folderId) {
    return COLORS[node.folderId % COLORS.length]
  }
  return '#6366f1'
}

let simulation: d3.Simulation<SimNode, SimEdge> | null = null
let zoomBehavior: d3.ZoomBehavior<SVGSVGElement, unknown> | null = null

async function loadGraph() {
  loading.value = true
  try {
    const data = await notesApi.graph()
    rawNodes.value = data.nodes
    rawEdges.value = data.edges
    await nextTick()
    renderGraph(data.nodes, data.edges)
  } catch (e) {
    console.error('Failed to load graph:', e)
  } finally {
    loading.value = false
  }
}

function renderGraph(nodes: GraphNode[], edges: GraphEdge[]) {
  if (!svgRef.value || !containerRef.value) return

  d3.select(svgRef.value).selectAll('*').remove()
  if (simulation) simulation.stop()

  const width = containerRef.value.clientWidth
  const height = containerRef.value.clientHeight

  const svg = d3.select(svgRef.value)
    .attr('width', width)
    .attr('height', height)

  const defs = svg.append('defs')
  const gradient = defs.append('radialGradient')
    .attr('id', 'node-glow')
  gradient.append('stop').attr('offset', '0%').attr('stop-color', 'var(--color-craft-accent)').attr('stop-opacity', 0.3)
  gradient.append('stop').attr('offset', '100%').attr('stop-color', 'var(--color-craft-accent)').attr('stop-opacity', 0)

  defs.append('marker')
    .attr('id', 'arrow')
    .attr('viewBox', '0 0 10 6')
    .attr('refX', 10)
    .attr('refY', 3)
    .attr('markerWidth', 8)
    .attr('markerHeight', 6)
    .attr('orient', 'auto')
    .append('path')
    .attr('d', 'M0,0 L10,3 L0,6 Z')
    .attr('fill', 'var(--color-craft-text-secondary)')
    .attr('opacity', 0.4)

  const g = svg.append('g')

  zoomBehavior = d3.zoom<SVGSVGElement, unknown>()
    .scaleExtent([0.1, 4])
    .on('zoom', (event) => {
      g.attr('transform', event.transform)
    })

  svg.call(zoomBehavior)
  svg.call(zoomBehavior.transform, d3.zoomIdentity.translate(width / 2, height / 2).scale(0.8))

  const linkCountMap = new Map<number, number>()
  edges.forEach(e => {
    linkCountMap.set(e.source, (linkCountMap.get(e.source) || 0) + 1)
    linkCountMap.set(e.target, (linkCountMap.get(e.target) || 0) + 1)
  })

  const simNodes: SimNode[] = nodes.map(n => ({
    ...n,
    linkCount: linkCountMap.get(n.id) || 0,
  }))

  const nodeMap = new Map(simNodes.map(n => [n.id, n]))

  const simEdges: SimEdge[] = edges
    .filter(e => nodeMap.has(e.source) && nodeMap.has(e.target))
    .map(e => ({
      source: nodeMap.get(e.source)!,
      target: nodeMap.get(e.target)!,
    }))

  simulation = d3.forceSimulation<SimNode>(simNodes)
    .force('link', d3.forceLink<SimNode, SimEdge>(simEdges).id(d => d.id).distance(120).strength(0.4))
    .force('charge', d3.forceManyBody().strength(-300).distanceMax(400))
    .force('center', d3.forceCenter(0, 0))
    .force('collision', d3.forceCollide<SimNode>().radius(d => getRadius(d) + 8))
    .force('x', d3.forceX(0).strength(0.02))
    .force('y', d3.forceY(0).strength(0.02))

  const linkGroup = g.append('g').attr('class', 'links')
  const nodeGroup = g.append('g').attr('class', 'nodes')

  const links = linkGroup.selectAll<SVGLineElement, SimEdge>('line')
    .data(simEdges)
    .join('line')
    .attr('stroke', 'var(--color-craft-border)')
    .attr('stroke-width', 1.5)
    .attr('stroke-opacity', 0.5)
    .attr('marker-end', 'url(#arrow)')

  const nodeGroups = nodeGroup.selectAll<SVGGElement, SimNode>('g')
    .data(simNodes)
    .join('g')
    .attr('cursor', 'pointer')
    .call(d3.drag<SVGGElement, SimNode>()
      .on('start', (event, d) => {
        if (!event.active) simulation!.alphaTarget(0.3).restart()
        d.fx = d.x
        d.fy = d.y
      })
      .on('drag', (event, d) => {
        d.fx = event.x
        d.fy = event.y
      })
      .on('end', (event, d) => {
        if (!event.active) simulation!.alphaTarget(0)
        d.fx = null
        d.fy = null
      })
    )

  nodeGroups.append('circle')
    .attr('r', d => getRadius(d) + 6)
    .attr('fill', 'url(#node-glow)')
    .attr('class', 'node-glow')
    .attr('opacity', 0)

  nodeGroups.append('circle')
    .attr('r', d => getRadius(d))
    .attr('fill', d => getNodeColor(d))
    .attr('stroke', '#fff')
    .attr('stroke-width', 2)
    .attr('class', 'node-circle')
    .style('filter', 'drop-shadow(0 1px 3px rgba(0,0,0,0.15))')

  nodeGroups.append('text')
    .text(d => truncateTitle(d.title || '无标题', 10))
    .attr('dy', d => getRadius(d) + 16)
    .attr('text-anchor', 'middle')
    .attr('fill', 'var(--color-craft-text)')
    .attr('font-size', '11px')
    .attr('font-weight', '500')
    .attr('pointer-events', 'none')
    .style('text-shadow', '0 1px 2px var(--color-craft-bg)')

  nodeGroups.on('mouseenter', function (_, d) {
    hoveredNode.value = d
    d3.select(this).select('.node-glow').transition().duration(200).attr('opacity', 1)
    d3.select(this).select('.node-circle').transition().duration(200).attr('r', getRadius(d) + 3)

    const connectedIds = new Set<number>()
    simEdges.forEach(e => {
      const src = (e.source as SimNode).id
      const tgt = (e.target as SimNode).id
      if (src === d.id) connectedIds.add(tgt)
      if (tgt === d.id) connectedIds.add(src)
    })

    nodeGroups.transition().duration(200)
      .attr('opacity', n => n.id === d.id || connectedIds.has(n.id) ? 1 : 0.15)
    links.transition().duration(200)
      .attr('stroke-opacity', e => (e.source as SimNode).id === d.id || (e.target as SimNode).id === d.id ? 0.8 : 0.05)
      .attr('stroke-width', e => (e.source as SimNode).id === d.id || (e.target as SimNode).id === d.id ? 2.5 : 1)
      .attr('stroke', e => (e.source as SimNode).id === d.id || (e.target as SimNode).id === d.id ? getNodeColor(d) : 'var(--color-craft-border)')
  })

  nodeGroups.on('mouseleave', function (_, d) {
    hoveredNode.value = null
    d3.select(this).select('.node-glow').transition().duration(300).attr('opacity', 0)
    d3.select(this).select('.node-circle').transition().duration(300).attr('r', getRadius(d))
    nodeGroups.transition().duration(300).attr('opacity', 1)
    links.transition().duration(300)
      .attr('stroke-opacity', 0.5)
      .attr('stroke-width', 1.5)
      .attr('stroke', 'var(--color-craft-border)')
  })

  nodeGroups.on('click', (_, d) => {
    selectedNode.value = d
    showPanel.value = true
  })

  simulation.on('tick', () => {
    links
      .attr('x1', d => (d.source as SimNode).x!)
      .attr('y1', d => (d.source as SimNode).y!)
      .attr('x2', d => {
        const s = d.source as SimNode, t = d.target as SimNode
        const dx = t.x! - s.x!, dy = t.y! - s.y!
        const dist = Math.sqrt(dx * dx + dy * dy) || 1
        return t.x! - (dx / dist) * (getRadius(t) + 4)
      })
      .attr('y2', d => {
        const s = d.source as SimNode, t = d.target as SimNode
        const dx = t.x! - s.x!, dy = t.y! - s.y!
        const dist = Math.sqrt(dx * dx + dy * dy) || 1
        return t.y! - (dy / dist) * (getRadius(t) + 4)
      })

    nodeGroups.attr('transform', d => `translate(${d.x},${d.y})`)
  })
}

function getRadius(d: SimNode): number {
  return Math.max(8, Math.min(24, 8 + d.linkCount * 3))
}

function truncateTitle(title: string, max: number): string {
  return title.length > max ? title.slice(0, max) + '...' : title
}

function navigateToNote(noteId: number) {
  noteStore.setActive(String(noteId))
  router.push({ name: 'notes' })
}

function zoomIn() {
  if (!svgRef.value || !zoomBehavior) return
  d3.select(svgRef.value).transition().duration(300).call(zoomBehavior.scaleBy, 1.3)
}

function zoomOut() {
  if (!svgRef.value || !zoomBehavior) return
  d3.select(svgRef.value).transition().duration(300).call(zoomBehavior.scaleBy, 0.7)
}

function zoomReset() {
  if (!svgRef.value || !zoomBehavior || !containerRef.value) return
  const width = containerRef.value.clientWidth
  const height = containerRef.value.clientHeight
  d3.select(svgRef.value).transition().duration(500)
    .call(zoomBehavior.transform, d3.zoomIdentity.translate(width / 2, height / 2).scale(0.8))
}

function handleSearchFocus(nodeId: number) {
  if (!svgRef.value || !zoomBehavior || !containerRef.value) return
  const svg = d3.select(svgRef.value)
  const width = containerRef.value.clientWidth
  const height = containerRef.value.clientHeight

  const nodeGroup = svg.selectAll<SVGGElement, SimNode>('g.nodes g')
  const target = nodeGroup.data().find(d => d.id === nodeId)
  if (!target || target.x == null || target.y == null) return

  svg.transition().duration(600)
    .call(zoomBehavior.transform,
      d3.zoomIdentity.translate(width / 2 - target.x * 1.2, height / 2 - target.y * 1.2).scale(1.2))

  selectedNode.value = target
  showPanel.value = true
}

const filteredNodes = computed(() => {
  const q = searchQuery.value.toLowerCase().trim()
  if (!q) return []
  return rawNodes.value.filter(n => (n.title || '').toLowerCase().includes(q)).slice(0, 10)
})

function getLinkedNotes(nodeId: number): GraphNode[] {
  const ids = new Set<number>()
  rawEdges.value.forEach(e => {
    if (e.source === nodeId) ids.add(e.target)
    if (e.target === nodeId) ids.add(e.source)
  })
  return rawNodes.value.filter(n => ids.has(n.id))
}

function handleResize() {
  if (rawNodes.value.length > 0) {
    renderGraph(rawNodes.value, rawEdges.value)
  }
}

onMounted(() => {
  loadGraph()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (simulation) simulation.stop()
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <div class="h-full flex flex-col bg-[var(--color-craft-bg)] overflow-hidden relative">
    <!-- Top bar -->
    <div class="shrink-0 px-6 py-4 flex items-center justify-between border-b border-[var(--color-craft-border)] bg-[var(--color-craft-surface)]">
      <div class="flex items-center gap-3">
        <div class="w-9 h-9 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center shadow-sm">
          <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round">
            <circle cx="5.5" cy="5.5" r="3"/>
            <circle cx="18.5" cy="5.5" r="3"/>
            <circle cx="12" cy="18.5" r="3"/>
            <line x1="8.5" y1="7" x2="9.5" y2="15.5"/>
            <line x1="15.5" y1="7" x2="14.5" y2="15.5"/>
            <line x1="8" y1="5.5" x2="15.5" y2="5.5"/>
          </svg>
        </div>
        <div>
          <h1 class="text-lg font-bold text-[var(--color-craft-text)]">知识图谱</h1>
          <p class="text-[11px] text-[var(--color-craft-text-secondary)]">
            {{ stats.totalNodes }} 篇笔记 · {{ stats.totalEdges }} 条链接 · {{ stats.connectedNodes }} 个连接节点
          </p>
        </div>
      </div>

      <div class="flex items-center gap-2">
        <!-- Search -->
        <div class="relative">
          <input
            v-model="searchQuery"
            placeholder="搜索笔记..."
            class="w-56 h-8 pl-8 pr-3 text-xs rounded-lg border border-[var(--color-craft-border)] bg-[var(--color-craft-bg)] text-[var(--color-craft-text)] outline-none focus:border-[var(--color-craft-accent)] focus:ring-1 focus:ring-[var(--color-craft-accent)]/30 transition-all"
          />
          <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="var(--color-craft-text-secondary)" stroke-width="2" stroke-linecap="round" class="absolute left-2.5 top-1/2 -translate-y-1/2">
            <circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>
          </svg>
          <!-- Search results dropdown -->
          <Transition name="fade-slide">
            <div
              v-if="filteredNodes.length > 0 && searchQuery.trim()"
              class="absolute top-full mt-1 left-0 w-72 bg-[var(--color-craft-surface)] border border-[var(--color-craft-border)] rounded-xl shadow-2xl z-50 overflow-hidden"
            >
              <div class="p-1.5 max-h-64 overflow-y-auto">
                <button
                  v-for="n in filteredNodes"
                  :key="n.id"
                  @click="handleSearchFocus(n.id); searchQuery = ''"
                  class="w-full flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm hover:bg-[var(--color-craft-hover)] transition-colors text-left"
                >
                  <span
                    class="w-2.5 h-2.5 rounded-full shrink-0"
                    :style="{ background: getNodeColor(n) }"
                  ></span>
                  <span class="truncate text-[var(--color-craft-text)]">{{ n.title || '无标题' }}</span>
                  <span v-if="n.tags.length" class="ml-auto text-[10px] text-[var(--color-craft-text-secondary)]">{{ n.tags[0] }}</span>
                </button>
              </div>
            </div>
          </Transition>
        </div>

        <!-- Zoom controls -->
        <div class="flex items-center bg-[var(--color-craft-bg)] border border-[var(--color-craft-border)] rounded-lg overflow-hidden">
          <button @click="zoomOut" class="w-7 h-7 flex items-center justify-center hover:bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)] transition-colors" title="缩小">
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="5" y1="12" x2="19" y2="12"/></svg>
          </button>
          <button @click="zoomReset" class="w-7 h-7 flex items-center justify-center border-x border-[var(--color-craft-border)] hover:bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)] transition-colors" title="重置缩放">
            <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
          </button>
          <button @click="zoomIn" class="w-7 h-7 flex items-center justify-center hover:bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)] transition-colors" title="放大">
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          </button>
        </div>

        <button
          @click="loadGraph"
          class="h-7 px-3 flex items-center gap-1.5 rounded-lg text-xs font-medium border border-[var(--color-craft-border)] hover:bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)] transition-all"
        >
          <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M21 12a9 9 0 0 0-9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5"/><path d="M3 12a9 9 0 0 0 9 9 9.75 9.75 0 0 0 6.74-2.74L21 16"/><path d="M16 16h5v5"/></svg>
          刷新
        </button>
      </div>
    </div>

    <!-- Graph canvas -->
    <div ref="containerRef" class="flex-1 relative overflow-hidden">
      <div v-if="loading" class="absolute inset-0 flex items-center justify-center bg-[var(--color-craft-bg)]/80 z-10">
        <div class="flex flex-col items-center gap-3">
          <div class="w-10 h-10 border-3 border-[var(--color-craft-accent)]/30 border-t-[var(--color-craft-accent)] rounded-full animate-spin"></div>
          <span class="text-sm text-[var(--color-craft-text-secondary)]">正在构建知识图谱...</span>
        </div>
      </div>

      <div
        v-if="!loading && rawNodes.length === 0"
        class="absolute inset-0 flex flex-col items-center justify-center"
      >
        <svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="var(--color-craft-text-secondary)" stroke-width="0.5" stroke-linecap="round" class="opacity-30 mb-4">
          <circle cx="5.5" cy="5.5" r="3"/><circle cx="18.5" cy="5.5" r="3"/><circle cx="12" cy="18.5" r="3"/>
          <line x1="8.5" y1="7" x2="9.5" y2="15.5"/><line x1="15.5" y1="7" x2="14.5" y2="15.5"/><line x1="8" y1="5.5" x2="15.5" y2="5.5"/>
        </svg>
        <p class="text-sm text-[var(--color-craft-text-secondary)]">暂无笔记数据</p>
        <p class="text-xs text-[var(--color-craft-text-secondary)] mt-1">在笔记中使用 [[ 创建双向链接，即可在此查看知识关联</p>
      </div>

      <svg ref="svgRef" class="w-full h-full"></svg>

      <!-- Hover tooltip -->
      <Transition name="fade">
        <div
          v-if="hoveredNode && !showPanel"
          class="absolute bottom-4 left-4 bg-[var(--color-craft-surface)] border border-[var(--color-craft-border)] rounded-xl shadow-lg px-4 py-3 pointer-events-none z-20 max-w-xs"
        >
          <div class="flex items-center gap-2 mb-1">
            <span class="w-3 h-3 rounded-full shrink-0" :style="{ background: getNodeColor(hoveredNode) }"></span>
            <span class="text-sm font-semibold text-[var(--color-craft-text)] truncate">{{ hoveredNode.title || '无标题' }}</span>
          </div>
          <div class="flex items-center gap-3 text-[11px] text-[var(--color-craft-text-secondary)]">
            <span>{{ (hoveredNode as SimNode).linkCount || 0 }} 个链接</span>
            <span v-if="hoveredNode.tags.length">{{ hoveredNode.tags.join(', ') }}</span>
          </div>
        </div>
      </Transition>
    </div>

    <!-- Detail panel -->
    <Transition name="slide-right">
      <div
        v-if="showPanel && selectedNode"
        class="absolute right-0 top-0 bottom-0 w-80 bg-[var(--color-craft-surface)] border-l border-[var(--color-craft-border)] shadow-2xl z-30 flex flex-col"
      >
        <div class="px-5 pt-5 pb-3 flex items-center justify-between border-b border-[var(--color-craft-border)]">
          <h3 class="text-sm font-bold text-[var(--color-craft-text)]">笔记详情</h3>
          <button @click="showPanel = false; selectedNode = null" class="w-6 h-6 flex items-center justify-center rounded-md hover:bg-[var(--color-craft-hover)] text-[var(--color-craft-text-secondary)] transition-colors">
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <div class="flex-1 overflow-y-auto p-5 space-y-4">
          <!-- Title -->
          <div>
            <div class="flex items-center gap-2 mb-2">
              <span class="w-4 h-4 rounded-full" :style="{ background: getNodeColor(selectedNode) }"></span>
              <h4 class="text-base font-bold text-[var(--color-craft-text)]">{{ selectedNode.title || '无标题' }}</h4>
            </div>
            <div class="flex items-center gap-2 text-[11px] text-[var(--color-craft-text-secondary)]">
              <span>{{ (selectedNode as SimNode).linkCount || 0 }} 个链接</span>
              <span v-if="selectedNode.updatedAt">· 更新于 {{ new Date(selectedNode.updatedAt).toLocaleDateString('zh-CN') }}</span>
            </div>
          </div>

          <!-- Tags -->
          <div v-if="selectedNode.tags.length" class="flex flex-wrap gap-1.5">
            <span
              v-for="tag in selectedNode.tags"
              :key="tag"
              class="px-2 py-0.5 text-[11px] rounded-full bg-[var(--color-craft-accent-light)] text-[var(--color-craft-accent)] font-medium"
            >#{{ tag }}</span>
          </div>

          <!-- Linked notes -->
          <div>
            <h5 class="text-[11px] font-semibold uppercase tracking-wider text-[var(--color-craft-text-secondary)] mb-2">关联笔记</h5>
            <div v-if="getLinkedNotes(selectedNode.id).length === 0" class="text-xs text-[var(--color-craft-text-secondary)] py-2">
              暂无关联笔记
            </div>
            <div v-else class="space-y-1">
              <button
                v-for="linked in getLinkedNotes(selectedNode.id)"
                :key="linked.id"
                @click="handleSearchFocus(linked.id)"
                class="w-full flex items-center gap-2.5 px-3 py-2 rounded-lg text-xs hover:bg-[var(--color-craft-hover)] transition-colors text-left"
              >
                <span class="w-2 h-2 rounded-full shrink-0" :style="{ background: getNodeColor(linked) }"></span>
                <span class="truncate text-[var(--color-craft-text)]">{{ linked.title || '无标题' }}</span>
              </button>
            </div>
          </div>

          <!-- Navigate button -->
          <button
            @click="navigateToNote(selectedNode.id)"
            class="w-full h-9 flex items-center justify-center gap-2 rounded-xl text-sm font-medium bg-[var(--color-craft-accent)] text-white hover:opacity-90 transition-all shadow-sm"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>
            打开笔记
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.fade-slide-enter-from,
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.slide-right-enter-active,
.slide-right-leave-active {
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
.slide-right-enter-from,
.slide-right-leave-to {
  transform: translateX(100%);
}

svg :deep(.nodes g:hover .node-circle) {
  filter: drop-shadow(0 2px 8px rgba(0,0,0,0.2));
}
</style>
