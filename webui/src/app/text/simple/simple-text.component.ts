import { Component, Input } from "@angular/core";

@Component({
  selector: 'app-simple-text',
  templateUrl: './simple-text.component.html',
  styleUrls: ['./simple-text.component.css']
})
export class SimpleTextComponent {
  @Input() text: string = '';

  constructor() {}

  getSegments(): string[] {
    return (this.text || '').trim()
    .replace(/<br\/>/ig, '')
    .replace(/\n/g, '\n<br/>\n')
    .replace(/(https?:\/\/[^\s]+)/g, `\n$&\n`)
    .split(/\n/);
  }

  isLineBreak(segment: string): boolean {
    return segment.startsWith('<br/>');
  }

  isUrl(segment: string): boolean {
    const lowerCaseSegment: string = segment.toLowerCase();
    return lowerCaseSegment.startsWith('http://') || lowerCaseSegment.startsWith('https://');
  }

  isPlainText(segment: string): boolean {
    return !this.isLineBreak(segment) && !this.isUrl(segment);
  }
}
