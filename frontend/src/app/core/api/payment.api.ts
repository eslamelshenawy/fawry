import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { RecommendRequest, RecommendResponse, SplitResponse } from '../models';

@Injectable({ providedIn: 'root' })
export class PaymentApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/payments`;

  recommend(request: RecommendRequest): Observable<RecommendResponse> {
    return this.http.post<RecommendResponse>(`${this.baseUrl}/recommend`, request);
  }

  split(request: RecommendRequest): Observable<SplitResponse> {
    return this.http.post<SplitResponse>(`${this.baseUrl}/split`, request);
  }
}
