import {Component, Inject} from "@angular/core";
import {MatDialog, MAT_DIALOG_DATA} from "@angular/material/dialog";
import {firstValueFrom} from "rxjs";

export interface TextDialogData {
  title: string;
  text: string;
  result: any;
}

@Component({
  selector: 'app-text-dialog',
  templateUrl: './text-dialog.component.html',
  styleUrls: [],
  standalone: false
})
export class TextDialog {

  text: string = "";

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: TextDialogData,
  ) {
  }

  static open(
    dialog: MatDialog,
    title: string,
    text: string,
  ): Promise<boolean> {
    return firstValueFrom(dialog.open(TextDialog, {
      maxWidth: "48em",
      maxHeight: "64em",
      data: {
        title,
        text,
        result: true,
      },
    }).afterClosed()) as Promise<boolean>;
  }
}
