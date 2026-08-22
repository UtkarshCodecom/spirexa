interface DateGroupProps {
  label: string;
}

export function DateGroup({ label }: DateGroupProps) {
  return (
    <div className="col-span-full flex items-center gap-4 py-4">
      <h2 className="text-lg font-semibold text-surface-900 whitespace-nowrap">{label}</h2>
      <div className="h-px flex-1 bg-surface-200" />
    </div>
  );
}
