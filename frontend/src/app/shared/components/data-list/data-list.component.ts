import { NgTemplateOutlet } from '@angular/common';
import { Component, ContentChild, Input, TemplateRef } from '@angular/core';

/**
 * Reusable list/table shell: renders a loading state, an empty state, or the caller's
 * row template for each item. Callers project a per-row template with `let-item`.
 *
 * Usage:
 * ```html
 * <app-data-list [items]="accounts()" [loading]="loading()" emptyMessage="No accounts yet.">
 *   <ng-template #row let-item>
 *     <tr><td>{{ item.id }}</td></tr>
 *   </ng-template>
 * </app-data-list>
 * ```
 */
@Component({
  selector: 'app-data-list',
  standalone: true,
  imports: [NgTemplateOutlet],
  templateUrl: './data-list.component.html',
  styleUrl: './data-list.component.css',
})
export class DataListComponent<T> {
  @Input({ required: true }) items: readonly T[] = [];
  @Input() loading = false;
  @Input() emptyMessage = 'Nothing to show yet.';

  @ContentChild('row') rowTemplate: TemplateRef<{ $implicit: T }> | null = null;
}
