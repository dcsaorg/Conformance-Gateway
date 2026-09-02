import { ChangeDetectorRef, Component, ChangeDetectionStrategy } from "@angular/core";
import { ConformanceService } from "../../service/conformance.service";
import { Router } from "@angular/router";
import { AuthService } from "../../auth/auth.service";
import { Sandbox } from "../../model/sandbox";
import { MatDialog } from "@angular/material/dialog";
import { ConfirmationDialog } from "../../dialogs/confirmation/confirmation-dialog.component";
import { MessageDialog } from "../../dialogs/message/message-dialog.component";

@Component({
    selector: 'app-environment',
    templateUrl: './environment.component.html',
    styleUrls: ['../../shared-styles.css', './environment.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class EnvironmentComponent {

  isLoading: boolean = true;
  sandboxes: Sandbox[] = [];
  filteredSandboxes: Sandbox[] = [];
  searchTerm: string = '';
  selectedSandboxIds = new Set<string>();
  isDeleting: boolean = false;

  constructor(
    public authService: AuthService,
    public conformanceService: ConformanceService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private dialog: MatDialog,
  ) {}

  async ngOnInit() {
    this.isLoading = true;
    if (!await this.authService.isAuthenticated()) {
      this.router.navigate([
        '/login'
      ]);
      return;
    }
    this.sandboxes = await this.conformanceService.getAllSandboxes();
    this.filterSandboxes();
    this.isLoading = false;
    this.cdr.detectChanges();
  }

  onSearchTermChange(searchTerm: string) {
    this.searchTerm = searchTerm;
    this.filterSandboxes();
  }

  private filterSandboxes() {
    const normalizedSearchTerm = this.searchTerm.trim().toLowerCase();
    this.filteredSandboxes = normalizedSearchTerm
      ? this.sandboxes.filter(sandbox => sandbox.name.toLowerCase().includes(normalizedSearchTerm))
      : this.sandboxes;
  }

  onSandboxClick(sandbox: Sandbox) {
    this.router.navigate([
      '/sandbox', sandbox.id
    ]);
  }

  isSandboxSelected(sandboxId: string): boolean {
    return this.selectedSandboxIds.has(sandboxId);
  }

  toggleSandboxSelection(sandboxId: string, selected: boolean) {
    if (selected) {
      this.selectedSandboxIds.add(sandboxId);
    } else {
      this.selectedSandboxIds.delete(sandboxId);
    }
  }

  areAllFilteredSandboxesSelected(): boolean {
    return this.filteredSandboxes.length > 0
      && this.filteredSandboxes.every(sandbox => this.selectedSandboxIds.has(sandbox.id));
  }

  areSomeFilteredSandboxesSelected(): boolean {
    const selectedCount = this.filteredSandboxes.filter(
      sandbox => this.selectedSandboxIds.has(sandbox.id)
    ).length;
    return selectedCount > 0 && selectedCount < this.filteredSandboxes.length;
  }

  toggleAllFilteredSandboxes(selected: boolean) {
    this.filteredSandboxes.forEach(sandbox => this.toggleSandboxSelection(sandbox.id, selected));
  }

  async onDeleteSelectedSandboxes() {
    if (this.isDeleting) {
      return;
    }

    const selectedSandboxes = this.sandboxes.filter(
      sandbox => this.selectedSandboxIds.has(sandbox.id)
    );
    if (selectedSandboxes.length === 0 || !await ConfirmationDialog.open(
      this.dialog,
      `Delete ${selectedSandboxes.length} ${selectedSandboxes.length === 1 ? 'sandbox' : 'sandboxes'}`,
      `Are you sure you want to delete the selected ${selectedSandboxes.length === 1 ? 'sandbox' : 'sandboxes'}? You cannot undo this operation.`,
    )) {
      return;
    }

    this.isDeleting = true;
    this.cdr.detectChanges();

    const results = await Promise.all(selectedSandboxes.map(async sandbox => {
      try {
        const response = await this.conformanceService.deleteSandbox(sandbox.id);
        return { sandbox, error: response?.error as string | undefined };
      } catch (error) {
        return {
          sandbox,
          error: error instanceof Error ? error.message : 'Unexpected error',
        };
      }
    }));

    const successfulIds = new Set(
      results.filter(result => !result.error).map(result => result.sandbox.id)
    );
    this.sandboxes = this.sandboxes.filter(sandbox => !successfulIds.has(sandbox.id));
    successfulIds.forEach(id => this.selectedSandboxIds.delete(id));
    this.filterSandboxes();
    this.isDeleting = false;
    this.cdr.detectChanges();

    const failures = results.filter(result => result.error);
    if (failures.length > 0) {
      const details = failures
        .map(({ sandbox, error }) => `${sandbox.name}: ${error}`)
        .join('\n');
      await MessageDialog.open(
        this.dialog,
        successfulIds.size > 0 ? 'Some sandboxes could not be deleted' : 'Sandboxes could not be deleted',
        details,
      );
    }
  }

  onCreateSandbox() {
    this.router.navigate([
      '/create-sandbox'
    ]);
  }
}
