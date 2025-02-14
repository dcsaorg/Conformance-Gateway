import { Component, Inject } from "@angular/core";
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from "@angular/material/dialog";

export interface MessageDialogData {
  title: string;
  message: string;
  result: any;
}

@Component({
    selector: 'app-message-dialog',
    templateUrl: './message-dialog.component.html',
    styleUrls: [],
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
    return await dialog.open(MessageDialog, {
      maxWidth: "48em",
      data: {
        title,
        message,
      },
    }).afterClosed().toPromise();
  }
}
