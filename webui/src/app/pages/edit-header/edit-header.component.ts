import {Component, Input} from "@angular/core";
import {HeaderNameAndValue} from "src/app/model/sandbox-config";

@Component({
    selector: 'app-edit-header',
    templateUrl: './edit-header.component.html',
    styleUrls: ['../../shared-styles.css'],
    standalone: false
})
export class EditHeaderComponent {
  @Input() headerNameAndValue!: HeaderNameAndValue;
}
