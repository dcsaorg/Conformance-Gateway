import { Component, Inject, ChangeDetectionStrategy } from "@angular/core";
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from "@angular/material/dialog";
import { firstValueFrom } from "rxjs";

export interface MessageDialogData {
  title: string;
  message: string;
  result: any;
}

@Component({
    selector: 'app-message-dialog',
    templateUrl: './message-dialog.component.html',
    styleUrls: [],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class MessageDialog {

  constructor(
    public dialogRef: MatDialogRef<MessageDialog>,
    @Inject(MAT_DIALOG_DATA) public data: MessageDialogData,
  ) {}

  static async open(
    dialog: MatDialog,
    title: string,
    message: string,
  ): Promise<void> {
    return await firstValueFrom(dialog.open(MessageDialog, {
      maxWidth: "48em",
      data: {
        title,
        message,
      },
    }).afterClosed());
  }

  static async showIfError(
    response: any,
    dialog: MatDialog,
    title: string,
  ): Promise<boolean> {
    if (response?.error) {
      await MessageDialog.open(dialog, title, response.error);
      return true;
    }
    return false;
  }
}
