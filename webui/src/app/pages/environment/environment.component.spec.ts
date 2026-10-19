import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { EnvironmentComponent } from './environment.component';
import { Sandbox } from '../../model/sandbox';
import { AuthService } from '../../auth/auth.service';
import { ConformanceService } from '../../service/conformance.service';

describe('EnvironmentComponent', () => {
  const sandboxes: Sandbox[] = [
    createSandbox('1', 'Alpha Booking'),
    createSandbox('2', 'Beta eBL'),
    createSandbox('3', 'Booking Gamma'),
  ];

  const authService = {
    isAuthenticated: jest.fn<() => Promise<boolean>>().mockResolvedValue(true),
  };
  const conformanceService = {
    getAllSandboxes: jest.fn<() => Promise<Sandbox[]>>().mockResolvedValue(sandboxes),
  };
  const router = {
    navigate: jest.fn(),
  };
  const changeDetectorRef = {
    detectChanges: jest.fn(),
  };
  let component: EnvironmentComponent;

  beforeEach(async () => {
    jest.restoreAllMocks();
    jest.clearAllMocks();
    component = new EnvironmentComponent(
      authService as any,
      conformanceService as any,
      router as any,
      changeDetectorRef as any,
    );
    await component.ngOnInit();
  });

  it('shows every sandbox after loading', () => {
    expect(component.filteredSandboxes).toEqual(sandboxes);
    expect(component.isLoading).toBe(false);
  });

  it('filters sandbox names by partial match without regard to case or surrounding whitespace', () => {
    component.onSearchTermChange('  BOOKING ');

    expect(component.filteredSandboxes.map(sandbox => sandbox.id)).toEqual(['1', '3']);
  });

  it('shows no sandboxes when no name matches', () => {
    component.onSearchTermChange('missing');

    expect(component.filteredSandboxes).toEqual([]);
  });

  it('restores every sandbox when the search is cleared', () => {
    component.onSearchTermChange('beta');
    component.onSearchTermChange('   ');

    expect(component.filteredSandboxes).toEqual(sandboxes);
  });

  it('filters only in the UI without fetching sandboxes again', () => {
    component.onSearchTermChange('alpha');
    component.onSearchTermChange('beta');

    expect(conformanceService.getAllSandboxes).toHaveBeenCalledTimes(1);
  });

  it('opens the chosen sandbox', () => {
    component.onSandboxClick(sandboxes[1]);

    expect(router.navigate).toHaveBeenCalledWith(['/sandbox', '2']);
  });

  it('opens sandbox creation', () => {
    component.onCreateSandbox();

    expect(router.navigate).toHaveBeenCalledWith(['/create-sandbox']);
  });

  it('renders sandbox navigation without selection or bulk-deletion controls', async () => {
    jest.spyOn(EnvironmentComponent.prototype, 'ngOnInit').mockResolvedValue();
    await TestBed.configureTestingModule({
      declarations: [EnvironmentComponent],
      imports: [FormsModule],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: ConformanceService, useValue: conformanceService },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(EnvironmentComponent);
    fixture.componentInstance.sandboxes = sandboxes;
    fixture.componentInstance.filteredSandboxes = sandboxes;
    fixture.componentInstance.isLoading = false;
    fixture.detectChanges();
    const page: HTMLElement = fixture.nativeElement;

    expect(page.querySelector('#select_all_sandboxes_checkbox')).toBeNull();
    expect(page.querySelector('#delete_selected_sandboxes_button')).toBeNull();
    expect(page.querySelector('mat-checkbox, input[type="checkbox"]')).toBeNull();
    expect(page.querySelectorAll('.sandboxCardLink')).toHaveLength(sandboxes.length);
    expect(page.querySelector('#sandbox_search_input')).not.toBeNull();

    page.querySelector<HTMLButtonElement>('.sandboxCardLink')!.click();
    expect(router.navigate).toHaveBeenCalledWith(['/sandbox', '1']);
    page.querySelector<HTMLButtonElement>('#create_sandbox_button')!.click();
    expect(router.navigate).toHaveBeenCalledWith(['/create-sandbox']);
  });
});

function createSandbox(id: string, name: string): Sandbox {
  return {
    id,
    name,
    standardName: 'Standard',
    standardVersion: '1.0.0',
    scenarioSuite: 'Suite',
    testedPartyRole: 'Carrier',
    isDefault: false,
    canNotifyParty: false,
    notificationsSuppressed: false,
    operatorLog: [],
  };
}


