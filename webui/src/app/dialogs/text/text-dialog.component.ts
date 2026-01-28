import {Component, Inject, ElementRef, OnDestroy} from "@angular/core";
import {MatDialog, MAT_DIALOG_DATA, MatDialogRef} from "@angular/material/dialog";
import {firstValueFrom} from "rxjs";

export interface TextDialogData {
  title: string;
  text: string;
  result: any;
}

@Component({
  selector: 'app-text-dialog',
  templateUrl: './text-dialog.component.html',
  styleUrls: ['./text-dialog.component.css'],
  standalone: false
})
export class TextDialog implements OnDestroy {

  text: string = "";
  private resizing = false;
  private startX = 0;
  private startY = 0;
  private startWidth = 0;
  private startHeight = 0;
  private dialogPane: HTMLElement | null = null;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: TextDialogData,
    private dialogRef: MatDialogRef<TextDialog>,
    private elementRef: ElementRef,
  ) {
    // Try to format and highlight JSON if the text is JSON
    this.text = this.formatText(data.text);
  }

  private formatText(text: string): string {
    try {
      // Try to parse as JSON and format it
      const parsed = JSON.parse(text);
      return JSON.stringify(parsed, null, 2);
    } catch {
      // Not JSON, return as is
      return text;
    }
  }

  get isJson(): boolean {
    try {
      JSON.parse(this.data.text);
      return true;
    } catch {
      return false;
    }
  }

  ngOnDestroy(): void {
    this.stopResize();
  }

  onResizeStart(event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();

    this.resizing = true;
    this.startX = event.clientX;
    this.startY = event.clientY;

    // Find the actual dialog pane element
    this.dialogPane = this.elementRef.nativeElement.closest('.cdk-overlay-pane');

    if (this.dialogPane) {
      const rect = this.dialogPane.getBoundingClientRect();
      this.startWidth = rect.width;
      this.startHeight = rect.height;

      document.addEventListener('mousemove', this.onResize);
      document.addEventListener('mouseup', this.onResizeEnd);
    }
  }

  private onResize = (event: MouseEvent): void => {
    if (!this.resizing || !this.dialogPane) return;

    const deltaX = event.clientX - this.startX;
    const deltaY = event.clientY - this.startY;

    const newWidth = Math.max(400, this.startWidth + deltaX);
    const newHeight = Math.max(300, this.startHeight + deltaY);

    this.dialogPane.style.width = `${newWidth}px`;
    this.dialogPane.style.height = `${newHeight}px`;
  };

  private onResizeEnd = (): void => {
    this.stopResize();
  };

  private stopResize(): void {
    this.resizing = false;
    document.removeEventListener('mousemove', this.onResize);
    document.removeEventListener('mouseup', this.onResizeEnd);
  }

  static open(
    dialog: MatDialog,
    title: string,
    text: string,
  ): Promise<boolean> {
    return firstValueFrom(dialog.open(TextDialog, {
      width: "600px",
      height: "500px",
      hasBackdrop: true,
      disableClose: false,
      data: {
        title,
        text,
        result: true,
      },
    }).afterClosed()) as Promise<boolean>;
  }
}
