import { RotateCcw } from 'lucide-react'
import { PageHeader } from '@/components/PageHeader'
import { Button } from '@/components/ui/button'
import { ChatConversation } from './ChatConversation'
import { useChat } from './ChatContext'

export function ChatPage() {
  const { turns, pending, reset } = useChat()
  return (
    <div className="mx-auto flex max-w-3xl flex-col">
      <PageHeader
        title="Ask"
        description="Ask about students, fees, attendance, feedback or the schedule. Answers are read from your data by an AI model running on this computer."
        actions={turns.length > 0 && (
          <Button variant="outline" onClick={reset} disabled={pending} data-testid="chat-new">
            <RotateCcw /> New chat
          </Button>
        )}
      />
      <ChatConversation />
    </div>
  )
}
