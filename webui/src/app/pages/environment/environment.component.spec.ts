import { beforeEach, describe, expect, it, jest } from '@jest/globals';
import { EnvironmentComponent } from './environment.component';
import { Sandbox } from '../../model/sandbox';
import { ConfirmationDialog } from '../../dialogs/confirmation/confirmation-dialog.component';
import { MessageDialog } from '../../dialogs/message/message-dialog.component';

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
    deleteSandbox: jest.fn<(sandboxId: string) => Promise<any>>(),
  };
  const router = {
    navigate: jest.fn(),
  };
  const changeDetectorRef = {
    detectChanges: jest.fn(),
  };
  const dialog = {};

  let component: EnvironmentComponent;

  beforeEach(async () => {
    jest.restoreAllMocks();
    jest.clearAllMocks();
    conformanceService.deleteSandbox.mockResolvedValue({});
    component = new EnvironmentComponent(
      authService as any,
      conformanceService as any,
      router as any,
      changeDetectorRef as any,
      dialog as any,
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

  it('selects and clears only the sandboxes displayed by the current filter', () => {
    component.toggleSandboxSelection('2', true);
    component.onSearchTermChange('booking');

    component.toggleAllFilteredSandboxes(true);

    expect([...component.selectedSandboxIds]).toEqual(['2', '1', '3']);
    expect(component.areAllFilteredSandboxesSelected()).toBe(true);

    component.toggleAllFilteredSandboxes(false);

    expect([...component.selectedSandboxIds]).toEqual(['2']);
    expect(component.areSomeFilteredSandboxesSelected()).toBe(false);
  });

  it('does not delete anything when bulk deletion is cancelled', async () => {
    jest.spyOn(ConfirmationDialog, 'open').mockResolvedValue(false);
    component.toggleSandboxSelection('1', true);

    await component.onDeleteSelectedSandboxes();

    expect(conformanceService.deleteSandbox).not.toHaveBeenCalled();
    expect(component.sandboxes).toEqual(sandboxes);
    expect(component.selectedSandboxIds.has('1')).toBe(true);
  });

  it('does not open a confirmation dialog when no sandboxes are selected', async () => {
    const confirmationDialog = jest.spyOn(ConfirmationDialog, 'open');

    await component.onDeleteSelectedSandboxes();

    expect(confirmationDialog).not.toHaveBeenCalled();
    expect(conformanceService.deleteSandbox).not.toHaveBeenCalled();
  });

  it('removes all successfully deleted sandboxes without showing an error', async () => {
    const confirmationDialog = jest.spyOn(ConfirmationDialog, 'open').mockResolvedValue(true);
    const messageDialog = jest.spyOn(MessageDialog, 'open').mockResolvedValue();
    component.toggleSandboxSelection('1', true);
    component.toggleSandboxSelection('2', true);

    await component.onDeleteSelectedSandboxes();

    expect(confirmationDialog).toHaveBeenCalledWith(
      dialog as any,
      'Delete 2 sandboxes',
      'Are you sure you want to delete the selected sandboxes? You cannot undo this operation.',
    );
    expect(component.sandboxes.map(sandbox => sandbox.id)).toEqual(['3']);
    expect(component.selectedSandboxIds.size).toBe(0);
    expect(messageDialog).not.toHaveBeenCalled();
  });

  it('removes successful deletions and keeps failed deletions selected for retry', async () => {
    jest.spyOn(ConfirmationDialog, 'open').mockResolvedValue(true);
    const messageDialog = jest.spyOn(MessageDialog, 'open').mockResolvedValue();
    conformanceService.deleteSandbox.mockImplementation(async sandboxId => {
      if (sandboxId === '2') {
        return { error: 'Deletion was rejected' };
      }
      if (sandboxId === '3') {
        throw new Error('Network unavailable');
      }
      return {};
    });
    component.toggleAllFilteredSandboxes(true);

    await component.onDeleteSelectedSandboxes();

    expect(conformanceService.deleteSandbox).toHaveBeenCalledTimes(3);
    expect(component.sandboxes.map(sandbox => sandbox.id)).toEqual(['2', '3']);
    expect(component.filteredSandboxes.map(sandbox => sandbox.id)).toEqual(['2', '3']);
    expect([...component.selectedSandboxIds]).toEqual(['2', '3']);
    expect(component.isDeleting).toBe(false);
    expect(messageDialog).toHaveBeenCalledWith(
      dialog as any,
      'Some sandboxes could not be deleted',
      'Beta eBL: Deletion was rejected\nBooking Gamma: Network unavailable',
    );
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


