import html2canvas from 'html2canvas'
import jsPDF from 'jspdf'

function fileName(base: string, ext: 'png' | 'pdf') {
  const safe = base.replace(/[^\w\u4e00-\u9fa5-]+/g, '_')
  return `${safe}_${new Date().getTime()}.${ext}`
}

export async function exportElementAsImage(el: HTMLElement, baseName: string) {
  const canvas = await renderCanvas(el)
  const url = canvas.toDataURL('image/png')
  const a = document.createElement('a')
  a.href = url
  a.download = fileName(baseName, 'png')
  a.click()
}

export async function exportElementAsPdf(el: HTMLElement, baseName: string) {
  const canvas = await renderCanvas(el)
  const pdf = new jsPDF('p', 'mm', 'a4')
  const pageWidthMm = 210
  const pageHeightMm = 297
  const marginMm = 6
  const contentWidthMm = pageWidthMm - marginMm * 2
  const contentHeightMm = pageHeightMm - marginMm * 2
  const pxPerMm = canvas.width / contentWidthMm
  const pageHeightPx = Math.floor(contentHeightMm * pxPerMm)

  let offsetY = 0
  let first = true
  while (offsetY < canvas.height) {
    const sliceHeight = Math.min(pageHeightPx, canvas.height - offsetY)
    const pageCanvas = document.createElement('canvas')
    pageCanvas.width = canvas.width
    pageCanvas.height = sliceHeight
    const ctx = pageCanvas.getContext('2d')
    if (!ctx) break
    ctx.drawImage(canvas, 0, offsetY, canvas.width, sliceHeight, 0, 0, canvas.width, sliceHeight)
    const imgData = pageCanvas.toDataURL('image/png')
    const drawHeightMm = sliceHeight / pxPerMm
    if (!first) {
      pdf.addPage()
    }
    pdf.addImage(imgData, 'PNG', marginMm, marginMm, contentWidthMm, drawHeightMm)
    offsetY += sliceHeight
    first = false
  }
  pdf.save(fileName(baseName, 'pdf'))
}

async function renderCanvas(el: HTMLElement) {
  await waitImagesLoaded(el)
  return html2canvas(el, {
    useCORS: true,
    allowTaint: false,
    scale: 2,
    backgroundColor: '#ffffff',
    width: el.scrollWidth,
    height: el.scrollHeight,
    windowWidth: el.scrollWidth,
    windowHeight: el.scrollHeight
  })
}

async function waitImagesLoaded(el: HTMLElement) {
  const imgs = Array.from(el.querySelectorAll('img'))
  const tasks = imgs.map((img) => {
    if (img.complete) return Promise.resolve()
    return new Promise<void>((resolve) => {
      img.onload = () => resolve()
      img.onerror = () => resolve()
    })
  })
  await Promise.all(tasks)
}

export function createExportContainer(innerHtml: string, width = 1100) {
  const host = document.createElement('div')
  host.style.position = 'fixed'
  host.style.left = '-10000px'
  host.style.top = '0'
  host.style.zIndex = '-1'
  host.style.width = `${width}px`
  host.style.background = '#fff'
  host.style.padding = '24px'
  host.innerHTML = innerHtml
  document.body.appendChild(host)
  return {
    el: host,
    destroy: () => {
      document.body.removeChild(host)
    }
  }
}
