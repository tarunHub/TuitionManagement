import { useEffect } from 'react'
import { useBlocker } from 'react-router-dom'

/**
 * Warns before leaving with unsaved changes: browser unload prompt plus an in-app navigation blocker.
 * Returns the blocker so the page can render its own confirm dialog.
 */
export function useUnsavedChangesWarning(dirty: boolean) {
  useEffect(() => {
    if (!dirty) return
    const handler = (e: BeforeUnloadEvent) => {
      e.preventDefault()
      e.returnValue = ''
    }
    window.addEventListener('beforeunload', handler)
    return () => window.removeEventListener('beforeunload', handler)
  }, [dirty])

  return useBlocker(({ currentLocation, nextLocation }) => dirty && currentLocation.pathname !== nextLocation.pathname)
}
