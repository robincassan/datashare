import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { LoginComponent } from './login';
import { AuthService } from '../../services/auth.service';
import { of, throwError } from 'rxjs';

describe('LoginComponent', () => {
  let component: LoginComponent;

  beforeEach(async () => {
    const authServiceMock = {
      login: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        provideRouter([])
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have empty email and password by default', () => {
    expect(component.email).toBe('');
    expect(component.password).toBe('');
    expect(component.error).toBe('');
  });

  it('should navigate to dashboard on successful login', () => {
    const authService = TestBed.inject(AuthService) as any;
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate');
    authService.login.mockReturnValue(of({ token: 'jwt' }));

    component.login();

    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should set error on login failure', () => {
    const authService = TestBed.inject(AuthService) as any;
    authService.login.mockReturnValue(throwError(() => ({ error: 'Identifiants invalides' })));

    component.login();

    expect(component.error).toBe('Identifiants invalides');
  });
});
