import { DOCUMENT } from '@angular/common';
import { Component, Inject } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { environment } from 'src/environments/environment';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.css'],
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
