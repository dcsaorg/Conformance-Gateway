import { Component, Inject } from "@angular/core";
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

  /**
   * Shows an error dialog if the response contains an error.
   * Note: Call cdr.detectChanges() after all state changes following this call.
   *
   * @param response - The API response to check for errors
   * @param dialog - MatDialog instance
   * @param title - Error dialog title
   * @returns true if an error was shown, false otherwise
   *
   * @example
   * const response = await this.service.doSomething();
   * if (await MessageDialog.showIfError(response, this.dialog, "Error doing something")) {
   *   this.isLoading = false;
   *   this.cdr.detectChanges();
   *   return;
   * }
   */
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
