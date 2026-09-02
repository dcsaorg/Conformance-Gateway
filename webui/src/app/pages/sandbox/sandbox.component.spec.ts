import { describe, expect, it } from '@jest/globals';
import { SandboxComponent } from './sandbox.component';
import { Sandbox } from '../../model/sandbox';

describe('SandboxComponent', () => {
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
});

function createComponent(): SandboxComponent {
  return new SandboxComponent(
    {} as any,
    {} as any,
    {} as any,
    {} as any,
    {} as any,
    {} as any,
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
