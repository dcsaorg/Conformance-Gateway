import { Component, Input } from "@angular/core";

@Component({
  selector: 'app-text-waiting',
  templateUrl: './text-waiting.component.html',
  styleUrls: ['../../shared-styles.css']
})
export class TextWaitingComponent {
  @Input() text: string = '';

  constructor() {}
}
