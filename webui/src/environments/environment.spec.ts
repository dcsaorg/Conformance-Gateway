import { environment } from './environment.testci';

describe('environment', () => {
  it('should have production defined', () => {
    expect(environment.production).toBeDefined();
  });
});
