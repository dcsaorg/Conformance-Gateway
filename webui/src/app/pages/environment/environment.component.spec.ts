import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { EnvironmentComponent } from './environment.component';
import { Sandbox } from '../../model/sandbox';

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


