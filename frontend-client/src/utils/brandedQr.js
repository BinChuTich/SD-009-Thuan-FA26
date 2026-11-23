const loadImage = (src) => new Promise((resolve, reject) => {
  const image = new Image()
  image.crossOrigin = 'anonymous'
  image.onload = () => resolve(image)
  image.onerror = reject
  image.src = src
})

export async function createBrandedQrDataUrl(content, brand = 'vietqr') {
  const qrUrl = `https://api.qrserver.com/v1/create-qr-code/?size=700x700&margin=32&format=png&ecc=H&data=${encodeURIComponent(content)}`
  const source = await loadImage(qrUrl)
  const canvas = document.createElement('canvas')
  canvas.width = 700
  canvas.height = 700
  const ctx = canvas.getContext('2d')
  ctx.drawImage(source, 0, 0, 700, 700)

  const boxSize = brand === 'momo' ? 126 : 118
  const x = (700 - boxSize) / 2
  const y = (700 - boxSize) / 2
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(x - 8, y - 8, boxSize + 16, boxSize + 16)

  if (brand === 'momo') {
    ctx.fillStyle = '#a50064'
    ctx.beginPath()
    ctx.roundRect(x, y, boxSize, boxSize, 22)
    ctx.fill()
    ctx.fillStyle = '#ffffff'
    ctx.font = '900 34px Arial, sans-serif'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText('momo', 350, 350)
  } else {
    ctx.fillStyle = '#e51f32'
    ctx.beginPath()
    ctx.moveTo(x + 20, y + 29)
    ctx.bezierCurveTo(x + 29, y + 22, x + 40, y + 25, x + 46, y + 36)
    ctx.lineTo(x + boxSize / 2, y + 77)
    ctx.lineTo(x + boxSize - 46, y + 36)
    ctx.bezierCurveTo(x + boxSize - 40, y + 25, x + boxSize - 29, y + 22, x + boxSize - 20, y + 29)
    ctx.bezierCurveTo(x + boxSize - 11, y + 36, x + boxSize - 10, y + 48, x + boxSize - 16, y + 58)
    ctx.lineTo(x + boxSize / 2 + 17, y + boxSize - 20)
    ctx.bezierCurveTo(x + boxSize / 2 + 10, y + boxSize - 8, x + boxSize / 2 - 10, y + boxSize - 8, x + boxSize / 2 - 17, y + boxSize - 20)
    ctx.lineTo(x + 16, y + 58)
    ctx.bezierCurveTo(x + 10, y + 48, x + 11, y + 36, x + 20, y + 29)
    ctx.fill()
  }

  return canvas.toDataURL('image/png')
}
