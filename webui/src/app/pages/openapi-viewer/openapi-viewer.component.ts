import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute } from "@angular/router";
import { Subscription } from "rxjs";
import SwaggerUI from 'swagger-ui-dist/swagger-ui-es-bundle';

@Component({
  selector: 'app-openapi-viewer',
  templateUrl: './openapi-viewer.component.html',
  styleUrls: ['./openapi-viewer.component.css']
})
export class OpenapiViewerComponent implements OnInit {

  activatedRouteSubscription: Subscription | undefined;

  constructor(
    public activatedRoute: ActivatedRoute,
  ) { }

  ngOnInit(): void {
    this.activatedRouteSubscription = this.activatedRoute.params.subscribe(
      async params => {
        const yamlFileName: string = params['yamlFileName'];
        SwaggerUI({
          dom_id: '#swagger-ui',
          url: 'assets/' + yamlFileName,
        });
      });
  }

  async ngOnDestroy() {
    if (this.activatedRouteSubscription) {
      this.activatedRouteSubscription.unsubscribe();
    }
  }

}
