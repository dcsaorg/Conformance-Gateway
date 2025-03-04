import { Component, Inject } from "@angular/core";
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from "@angular/material/dialog";

export interface ConfirmationDialogData {
  title: string;
  question: string;
  requiredConfirmationString: string | undefined;
  result: any;
}

@Component({
    selector: 'app-confirmation-dialog',
    templateUrl: './confirmation-dialog.component.html',
    styleUrls: [],
    standalone: false
})
export class ConfirmationDialog {

  confirmationText: string = "";

  constructor(
    public dialogRef: MatDialogRef<ConfirmationDialog>,
    @Inject(MAT_DIALOG_DATA) public data: ConfirmationDialogData,
  ) {}

  onCancelClick() {
    this.dialogRef.close();
  }

  static async open(
    dialog: MatDialog,
    title: string,
    question: string,
    requiredConfirmationString?: string,
  ): Promise<boolean> {
    return await dialog.open(ConfirmationDialog, {
      maxWidth: "48em",
      data: {
        title,
        question,
        requiredConfirmationString,
        result: true,
      },
    }).afterClosed().toPromise();
  }
}
