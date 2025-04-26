import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'sort'
})
export class SortPipe implements PipeTransform {
  transform(array: any[], field: string, ascending: boolean = true): any[] {
    if (!array || !field) {
      return array;
    }

    return array.sort((a, b) => {
      const valA = field.split('.').reduce((o, i) => o[i], a);
      const valB = field.split('.').reduce((o, i) => o[i], b);

      if (valA < valB) {
        return ascending ? -1 : 1;
      }
      if (valA > valB) {
        return ascending ? 1 : -1;
      }
      return 0;
    });
  }
}
