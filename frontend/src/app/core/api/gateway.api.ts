import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Gateway } from '../models';

@Injectable({ providedIn: 'root' })
export class GatewayApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/gateways`;

  list(): Observable<Gateway[]> {
    return this.http.get<Gateway[]>(this.baseUrl);
  }
}
