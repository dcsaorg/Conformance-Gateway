import { Component, Input, ChangeDetectionStrategy } from "@angular/core";

@Component({
    selector: 'app-text-waiting',
    templateUrl: './text-waiting.component.html',
    styleUrls: ['../../shared-styles.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class TextWaitingComponent {
  @Input() text: string = '';

  constructor() {}
}
