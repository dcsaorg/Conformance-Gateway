import {Component, Input} from "@angular/core";
import {Sandbox} from "../../model/sandbox";

@Component({
  selector: 'app-sandbox-type',
  templateUrl: './sandbox-type.component.html',
  styleUrls: ['../../shared-styles.css'],
  standalone: false
})
export class SandboxTypeComponent {
  @Input() sandbox: Sandbox | undefined;
}
