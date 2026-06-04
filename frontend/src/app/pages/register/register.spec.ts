import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RegisterComponent } from './register';
import { AuthService } from '../../services/auth.service';
import { of, throwError } from 'rxjs';

describe('RegisterComponent', () => {
  let component: RegisterComponent;

  beforeEach(async () => {
    const authServiceMock = {
      register: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        provideRouter([])
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have empty fields by default', () => {
    expect(component.email).toBe('');
    expect(component.password).toBe('');
    expect(component.confirmPassword).toBe('');
  });

  it('should reject registration when passwords do not match', () => {
    component.password = '12345678';
    component.confirmPassword = 'different';
    component.register();
    expect(component.error).toBe('Les mots de passe ne correspondent pas');
  });

  it('should navigate to login on successful registration', () => {
    const authService = TestBed.inject(AuthService) as any;
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate');
    authService.register.mockReturnValue(of({}));

    component.email = 'test@test.com';
    component.password = '12345678';
    component.confirmPassword = '12345678';
    component.register();

    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should set error on registration failure', () => {
    const authService = TestBed.inject(AuthService) as any;
    authService.register.mockReturnValue(throwError(() => ({ error: 'Email déjà utilisé' })));

    component.email = 'test@test.com';
    component.password = '12345678';
    component.confirmPassword = '12345678';
    component.register();

    expect(component.error).toBe('Email déjà utilisé');
  });
});
