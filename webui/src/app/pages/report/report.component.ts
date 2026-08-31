import { Component, Input, ChangeDetectionStrategy } from "@angular/core";
import { getConformanceStatusEmoji,
  getConformanceStatusTitle
} from "src/app/model/conformance-status";
import { ScenarioConformanceReport } from "src/app/model/scenario-status";

@Component({
    selector: 'app-report',
    templateUrl: './report.component.html',
    styleUrls: ['../../shared-styles.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class ReportComponent {
  @Input() unfoldedLevels!: number;
  @Input() report!: ScenarioConformanceReport;

  getConformanceStatusEmoji = getConformanceStatusEmoji;
  getConformanceStatusTitle = getConformanceStatusTitle;

  constructor(
  ) {}
}
