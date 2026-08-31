import { ChangeDetectorRef, Component, ChangeDetectionStrategy } from "@angular/core";
import { ConformanceService } from "../../service/conformance.service";
import { Router } from "@angular/router";
import { AuthService } from "../../auth/auth.service";
import { Sandbox } from "../../model/sandbox";

@Component({
    selector: 'app-environment',
    templateUrl: './environment.component.html',
    styleUrls: ['../../shared-styles.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class EnvironmentComponent {

  isLoading: boolean = true;
  sandboxes: Sandbox[] = [];
  filteredSandboxes: Sandbox[] = [];
  searchTerm: string = '';

  constructor(
    public authService: AuthService,
    public conformanceService: ConformanceService,
    private router: Router,
    private cdr: ChangeDetectorRef,
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

  onCreateSandbox() {
    this.router.navigate([
      '/create-sandbox'
    ]);
  }
}
