import {Component, Input, ChangeDetectionStrategy} from "@angular/core";
import {Sandbox} from "../../model/sandbox";

@Component({
  selector: 'app-sandbox-type',
  templateUrl: './sandbox-type.component.html',
  styleUrls: ['../../shared-styles.css'],
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: false
})
export class SandboxTypeComponent {
  @Input() sandbox: Sandbox | undefined;
}
