import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should register a user', () => {
    service.register('test@test.com', '12345678').subscribe(res => {
      expect(res).toEqual({ email: 'test@test.com' });
    });

    const req = httpMock.expectOne('http://localhost:8080/api/auth/register');
    expect(req.request.body).toEqual({ email: 'test@test.com', password: '12345678' });
    req.flush({ email: 'test@test.com' });
  });

  it('should login and store token', () => {
    service.login('test@test.com', '12345678').subscribe(res => {
      expect(res.token).toBe('jwt-token');
    });

    const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
    expect(req.request.body).toEqual({ email: 'test@test.com', password: '12345678' });
    req.flush({ token: 'jwt-token' });

    expect(localStorage.getItem('jwt_token')).toBe('jwt-token');
  });

  it('should return token from localStorage', () => {
    localStorage.setItem('jwt_token', 'stored-token');
    expect(service.getToken()).toBe('stored-token');
  });

  it('should return null when no token', () => {
    expect(service.getToken()).toBeNull();
  });

  it('should return true when logged in', () => {
    localStorage.setItem('jwt_token', 'token');
    expect(service.isLoggedIn()).toBeTruthy();
  });

  it('should return false when not logged in', () => {
    expect(service.isLoggedIn()).toBeFalsy();
  });

  it('should logout and remove token', () => {
    localStorage.setItem('jwt_token', 'token');
    service.logout();
    expect(localStorage.getItem('jwt_token')).toBeNull();
  });
});
