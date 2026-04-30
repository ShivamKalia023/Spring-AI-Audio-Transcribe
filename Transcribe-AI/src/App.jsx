import { useState } from 'react'
import './App.css'
import AudioUploader from './AudioUploader'
import RealTimeListener from './RealTimeListener'

function App() {
  const [mode, setMode] = useState('realtime')

  return (
    <>
      <div className="mode-tabs">
        <button
          className={`tab-btn${mode === 'realtime' ? ' active' : ''}`}
          onClick={() => setMode('realtime')}
        >
          🎙️ Real-Time
        </button>
        <button
          className={`tab-btn${mode === 'upload' ? ' active' : ''}`}
          onClick={() => setMode('upload')}
        >
          📁 File Upload
        </button>
      </div>
      {mode === 'realtime' ? <RealTimeListener /> : <AudioUploader />}
    </>
  )
}

export default App
