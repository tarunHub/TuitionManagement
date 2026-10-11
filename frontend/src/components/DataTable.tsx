import { useState, type ReactNode } from 'react'
import {
  flexRender,
  getCoreRowModel,
  getFilteredRowModel,
  getPaginationRowModel,
  getSortedRowModel,
  useReactTable,
  type ColumnDef,
  type OnChangeFn,
  type PaginationState,
  type Row,
  type RowSelectionState,
  type SortingState,
  type VisibilityState,
} from '@tanstack/react-table'
import { ArrowDown, ArrowUp, ArrowUpDown, Columns3, Search } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuLabel,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Input } from '@/components/ui/input'
import { Table, TableBody, TableCell, TableFooter, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { EmptyState, TableSkeleton } from '@/components/States'
import { cn } from '@/lib/utils'

/** Server-side paging/sorting; omit for client-side mode. */
export interface ServerSideOptions {
  pageCount: number
  totalElements: number
  pagination: PaginationState
  onPaginationChange: OnChangeFn<PaginationState>
  sorting: SortingState
  onSortingChange: OnChangeFn<SortingState>
}

interface DataTableProps<T> {
  columns: ColumnDef<T, any>[]
  data: T[]
  loading?: boolean
  emptyTitle?: string
  emptyDescription?: ReactNode
  /** Controlled search box; in client mode it filters globally when `onSearchChange` is omitted. */
  searchValue?: string
  onSearchChange?: (value: string) => void
  searchPlaceholder?: string
  searchable?: boolean
  toolbar?: ReactNode
  server?: ServerSideOptions
  pageSize?: number
  paginate?: boolean
  initialVisibility?: VisibilityState
  rowSelection?: RowSelectionState
  onRowSelectionChange?: OnChangeFn<RowSelectionState>
  enableRowSelection?: (row: Row<T>) => boolean
  getRowId?: (row: T) => string
  rowTestId?: (row: T) => string
  onRowClick?: (row: T) => void
  /** Footer row values keyed by column id (e.g. a TOTAL row); stays aligned when columns are hidden. */
  footerRow?: Record<string, ReactNode>
  testId?: string
}

/** Reusable table: search, sort, pagination, column visibility, loading and empty states. */
export function DataTable<T>({
  columns,
  data,
  loading,
  emptyTitle = 'Nothing here yet',
  emptyDescription,
  searchValue,
  onSearchChange,
  searchPlaceholder = 'Search…',
  searchable = true,
  toolbar,
  server,
  pageSize = 20,
  paginate = true,
  initialVisibility,
  rowSelection,
  onRowSelectionChange,
  enableRowSelection,
  getRowId,
  rowTestId,
  onRowClick,
  footerRow,
  testId = 'data-table',
}: DataTableProps<T>) {
  const [clientSorting, setClientSorting] = useState<SortingState>([])
  const [globalFilter, setGlobalFilter] = useState('')
  const [clientPagination, setClientPagination] = useState<PaginationState>({ pageIndex: 0, pageSize })
  const [visibility, setVisibility] = useState<VisibilityState>(initialVisibility ?? {})

  const table = useReactTable({
    data,
    columns,
    getRowId,
    state: {
      sorting: server ? server.sorting : clientSorting,
      pagination: server ? server.pagination : clientPagination,
      globalFilter: server ? undefined : globalFilter,
      columnVisibility: visibility,
      rowSelection: rowSelection ?? {},
    },
    manualPagination: !!server,
    manualSorting: !!server,
    manualFiltering: !!server,
    pageCount: server?.pageCount,
    onSortingChange: server ? server.onSortingChange : setClientSorting,
    onPaginationChange: server ? server.onPaginationChange : setClientPagination,
    onGlobalFilterChange: setGlobalFilter,
    onColumnVisibilityChange: setVisibility,
    onRowSelectionChange,
    enableRowSelection: enableRowSelection ?? !!onRowSelectionChange,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: server ? undefined : getSortedRowModel(),
    getFilteredRowModel: server ? undefined : getFilteredRowModel(),
    getPaginationRowModel: server || !paginate ? undefined : getPaginationRowModel(),
  })

  const search = onSearchChange ? searchValue ?? '' : globalFilter
  const setSearch = onSearchChange ?? setGlobalFilter
  const rows = table.getRowModel().rows
  const total = server ? server.totalElements : table.getFilteredRowModel().rows.length
  const { pageIndex, pageSize: size } = table.getState().pagination
  const showPager = paginate && total > size

  return (
    <div className="space-y-3" data-testid={testId}>
      <div className="no-print flex flex-wrap items-center gap-2">
        {searchable && (
          <div className="relative w-full sm:w-72">
            <Search className="pointer-events-none absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" aria-hidden />
            <Input
              aria-label="Search"
              placeholder={searchPlaceholder}
              value={search}
              onChange={(e) => {
                setSearch(e.target.value)
                table.setPageIndex(0)
              }}
              className="pl-8"
              data-testid="table-search"
            />
          </div>
        )}
        {toolbar}
        <div className="ml-auto">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="outline" size="sm" data-testid="column-visibility">
                <Columns3 /> Columns
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuLabel>Show columns</DropdownMenuLabel>
              {table
                .getAllLeafColumns()
                .filter((c) => c.getCanHide())
                .map((c) => (
                  <DropdownMenuCheckboxItem key={c.id} checked={c.getIsVisible()} onCheckedChange={(v) => c.toggleVisibility(!!v)} onSelect={(e) => e.preventDefault()}>
                    {typeof c.columnDef.header === 'string' ? c.columnDef.header : c.id}
                  </DropdownMenuCheckboxItem>
                ))}
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>

      {loading ? (
        <TableSkeleton />
      ) : rows.length === 0 ? (
        <EmptyState title={emptyTitle} description={emptyDescription} />
      ) : (
        <div className="rounded-lg border bg-card">
          <Table>
            <TableHeader>
              {table.getHeaderGroups().map((hg) => (
                <TableRow key={hg.id}>
                  {hg.headers.map((h) => {
                    const sortable = h.column.getCanSort()
                    const sorted = h.column.getIsSorted()
                    return (
                      <TableHead
                        key={h.id}
                        aria-sort={sorted === 'asc' ? 'ascending' : sorted === 'desc' ? 'descending' : undefined}
                        className={cn((h.column.columnDef.meta as { className?: string } | undefined)?.className)}
                      >
                        {h.isPlaceholder ? null : sortable ? (
                          <button type="button" className="inline-flex items-center gap-1 uppercase hover:text-foreground" onClick={h.column.getToggleSortingHandler()}>
                            {flexRender(h.column.columnDef.header, h.getContext())}
                            {sorted === 'asc' ? <ArrowUp className="h-3 w-3" /> : sorted === 'desc' ? <ArrowDown className="h-3 w-3" /> : <ArrowUpDown className="h-3 w-3 opacity-40" />}
                          </button>
                        ) : (
                          flexRender(h.column.columnDef.header, h.getContext())
                        )}
                      </TableHead>
                    )
                  })}
                </TableRow>
              ))}
            </TableHeader>
            <TableBody>
              {rows.map((row) => (
                <TableRow
                  key={row.id}
                  data-state={row.getIsSelected() ? 'selected' : undefined}
                  data-testid={rowTestId ? rowTestId(row.original) : 'table-row'}
                  className={cn(onRowClick && 'cursor-pointer')}
                  onClick={onRowClick ? () => onRowClick(row.original) : undefined}
                >
                  {row.getVisibleCells().map((cell) => (
                    <TableCell key={cell.id} className={cn((cell.column.columnDef.meta as { className?: string } | undefined)?.className)}>
                      {flexRender(cell.column.columnDef.cell, cell.getContext())}
                    </TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
            {footerRow && (
              <TableFooter data-testid={`${testId}-footer`}>
                <TableRow>
                  {table.getVisibleLeafColumns().map((c) => (
                    <TableCell key={c.id} className={cn((c.columnDef.meta as { className?: string } | undefined)?.className)}>
                      {footerRow[c.id] ?? null}
                    </TableCell>
                  ))}
                </TableRow>
              </TableFooter>
            )}
          </Table>
        </div>
      )}

      {showPager && !loading && (
        <div className="no-print flex items-center justify-between text-sm text-muted-foreground" data-testid="pagination">
          <span>
            {pageIndex * size + 1}–{Math.min((pageIndex + 1) * size, total)} of {total}
          </span>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={() => table.previousPage()} disabled={!table.getCanPreviousPage()}>
              Previous
            </Button>
            <Button variant="outline" size="sm" onClick={() => table.nextPage()} disabled={!table.getCanNextPage()}>
              Next
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
