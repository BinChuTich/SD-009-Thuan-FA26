import { spawn } from 'child_process'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const clientDir = path.resolve(__dirname, '../frontend-client')

console.log('\x1b[36m%s\x1b[0m', '=======================================================')
console.log('\x1b[32m%s\x1b[0m', ' 🚀 ĐANG KHỞI ĐỘNG CẢ 2 HỆ THỐNG: ADMIN VÀ ONLINE SHOP ')
console.log('\x1b[36m%s\x1b[0m', '=======================================================\n')

const isWin = process.platform === 'win32'
const npmCmd = isWin ? 'npm.cmd' : 'npm'

// 1. Chạy Admin (Cổng 5173)
const adminProcess = spawn(npmCmd, ['run', 'dev:admin'], {
  cwd: __dirname,
  stdio: 'inherit',
  shell: isWin
})

// 2. Chạy Online Client (Cổng 5174)
const clientProcess = spawn(npmCmd, ['run', 'dev'], {
  cwd: clientDir,
  stdio: 'inherit',
  shell: isWin
})

const handleExit = () => {
  try {
    adminProcess.kill()
  } catch (e) {}
  try {
    clientProcess.kill()
  } catch (e) {}
  process.exit()
}

process.on('SIGINT', handleExit)
process.on('SIGTERM', handleExit)
process.on('exit', handleExit)

