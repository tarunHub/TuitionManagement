import { platformApi } from '@/api/platformApi'
import { PageHeader } from '@/components/PageHeader'
import { UsersPanel } from './UsersPanel'

/** Logins that can onboard centres, manage every login and open any centre. */
export function PlatformAdminsPage() {
  return (
    <div>
      <PageHeader
        title="Platform admins"
        description="These logins can add centres, manage every centre's logins, and open any centre to see its data."
      />
      <UsersPanel
        queryKey={['platform', 'admins']}
        load={platformApi.admins}
        add={platformApi.addAdmin}
        addLabel="Add platform admin"
        emptyText="No platform admins."
      />
    </div>
  )
}
