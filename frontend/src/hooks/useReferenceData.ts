import { useQuery } from '@tanstack/react-query'
import { classGroupsApi, gradeRatesApi } from '@/api/referenceApi'

export function useGradeRates() {
  return useQuery({ queryKey: ['grade-rates'], queryFn: gradeRatesApi.list, staleTime: 60_000 })
}

export function useClassGroups() {
  return useQuery({ queryKey: ['class-groups'], queryFn: classGroupsApi.list, staleTime: 60_000 })
}
