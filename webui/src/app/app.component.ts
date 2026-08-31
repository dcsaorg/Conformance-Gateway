import { DOCUMENT } from '@angular/common';
import { Component, Inject, ChangeDetectionStrategy } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { environment } from 'src/environments/environment';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class AppComponent {
  date = new Date();
  title = 'webui';
  public constructor(
    @Inject(DOCUMENT) private document: Document,
    private titleService: Title,
  ) {}

  ngOnInit() {
    this.titleService.setTitle(environment.siteTitle);
  }
}
