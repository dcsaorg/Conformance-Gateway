import { Component, Input } from "@angular/core";

@Component({
    selector: 'app-text-waiting',
    templateUrl: './text-waiting.component.html',
    styleUrls: ['../../shared-styles.css'],
    standalone: false
})
export class TextWaitingComponent {
  @Input() text: string = '';

  constructor() {}
}
