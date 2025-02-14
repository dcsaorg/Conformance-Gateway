import { Component, Input } from "@angular/core";
import { getConformanceStatusEmoji,
  getConformanceStatusTitle
} from "src/app/model/conformance-status";
import { ScenarioConformanceReport } from "src/app/model/scenario-status";

@Component({
  selector: 'app-report',
  templateUrl: './report.component.html',
  styleUrls: ['../../shared-styles.css']
})
export class ReportComponent {
  @Input() folded!: boolean;
  @Input() report!: ScenarioConformanceReport;

  getConformanceStatusEmoji = getConformanceStatusEmoji;
  getConformanceStatusTitle = getConformanceStatusTitle;

  constructor(
  ) {}
}
