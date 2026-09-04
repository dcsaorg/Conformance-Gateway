import { afterEach, describe, expect, it, jest } from '@jest/globals';
import { SandboxComponent } from './sandbox.component';
import { Sandbox } from '../../model/sandbox';
import { ConfirmationDialog } from '../../dialogs/confirmation/confirmation-dialog.component';
import { MessageDialog } from '../../dialogs/message/message-dialog.component';

describe('SandboxComponent', () => {
  afterEach(() => {
    jest.useRealTimers();
    jest.restoreAllMocks();
  });

  it('shows the no-scenarios state only for a loaded regular sandbox with no modules', () => {
    const component = createComponent();
    component.sandbox = createSandbox(false);
    component.isLoading = false;

    expect(component.shouldShowNoScenarios()).toBe(true);

    component.isLoading = true;
    expect(component.shouldShowNoScenarios()).toBe(false);
  });

  it('does not show the no-scenarios state for an internal sandbox', () => {
    const component = createComponent();
    component.sandbox = createSandbox(true);
    component.isLoading = false;

    expect(component.isInternalSandbox()).toBe(true);
    expect(component.shouldShowNoScenarios()).toBe(false);
  });

  it('automatically refreshes internal sandbox activity without clearing the page', async () => {
    jest.useFakeTimers();
    const refreshedSandbox = createSandbox(true);
    refreshedSandbox.operatorLog = ['new activity'];
    const getSandbox =
      jest.fn<(sandboxId: string, includeOperatorLog: boolean) => Promise<Sandbox>>()
        .mockResolvedValue(refreshedSandbox);
    const component = createComponent({ getSandbox });
    component.sandboxId = 'sandbox-id';
    component.sandbox = createSandbox(true);

    (component as any).startInternalSandboxAutoRefresh();
    expect(component.sandbox).toBeDefined();
    await jest.advanceTimersByTimeAsync(2_000);

    expect(getSandbox).toHaveBeenCalledWith('sandbox-id', true);
    expect(component.sandbox).toBe(refreshedSandbox);
    component.ngOnDestroy();
  });

  it('resets an internal party and updates its state in place', async () => {
    const refreshedSandbox = createSandbox(true);
    refreshedSandbox.operatorLog = ['party reset'];
    const resetParty = jest.fn<(sandboxId: string) => Promise<any>>().mockResolvedValue({});
    const getSandbox =
      jest.fn<(sandboxId: string, includeOperatorLog: boolean) => Promise<Sandbox>>()
        .mockResolvedValue(refreshedSandbox);
    jest.spyOn(ConfirmationDialog, 'open').mockResolvedValue(true);
    jest.spyOn(MessageDialog, 'showIfError').mockResolvedValue(false);
    const component = createComponent({ resetParty, getSandbox });
    component.sandboxId = 'sandbox-id';
    component.sandbox = createSandbox(true);

    await component.onClickResetParty();

    expect(resetParty).toHaveBeenCalledWith('sandbox-id');
    expect(getSandbox).toHaveBeenCalledWith('sandbox-id', true);
    expect(component.sandbox).toBe(refreshedSandbox);
  });
});

function createComponent(conformanceService: Record<string, any> = {}): SandboxComponent {
  return new SandboxComponent(
    {} as any,
    {} as any,
    conformanceService as any,
    {} as any,
    {} as any,
    { detectChanges: jest.fn() } as any,
  );
}

function createSandbox(canNotifyParty: boolean): Sandbox {
  return {
    id: 'sandbox-id',
    name: 'Sandbox',
    standardName: 'Standard',
    standardVersion: '1.0.0',
    scenarioSuite: 'Suite',
    testedPartyRole: 'Carrier',
    isDefault: false,
    canNotifyParty,
    notificationsSuppressed: false,
    operatorLog: [],
  };
}
